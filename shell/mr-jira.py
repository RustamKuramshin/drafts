#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
mr-jira.py — CLI-инструмент для извлечения Jira-задач из Merge Request в GitLab.

Ключевые возможности:
- Подключение к GitLab и Jira с аутентификацией по токенам/учётным данным.
- Парсинг MR-URL, получение коммитов MR и фильтрация «служебных» коммитов (merge/revert/wip/skip ci).
- Извлечение Jira-ключей из сообщений коммитов по регулярному выражению.
- Нахождение «корневых» задач в Jira (поднимаемся по полю parent у подзадач), дедупликация.
- Удобный CLI с иерархией команд: `get issues <MR_URL>` и богатыми опциями/справкой.

Требуемые библиотеки (установите при необходимости):
  conda create -n mr-jira python=3.12
  conda activate mr-jira
  python -m pip install python-gitlab
  python -m pip install "typer[all]"
  python -m pip install jira

Переменные окружения по умолчанию:
- GITLAB_TOKEN — токен для GitLab (private token или PAT)
- JIRA_TOKEN — токен для Jira (если не используется basic_auth)
- JIRA_BASE  — базовый URL Jira (по умолчанию https://track.magnit.ru)
- JIRA_KEY_RE — regexp для Jira-ключей (по умолчанию [A-Z][A-Z0-9]+-[0-9]+)
- USER_AGENT — User-Agent для Jira-запросов (при использовании прямого HTTP — не требуется с библиотеками)

Примеры:
- Помощь:
    ./mr-jira.py get issues --help
- Получить список задач:
    ./mr-jira.py get issues https://gitlab.platform.corp/magnitonline/mm/backend/ke-backend/-/merge_requests/1808
    ./mr-jira.py get issues https://gitlab.platform.corp/magnitonline/mm/backend/ke-backend/-/merge_requests/1808 --jira-project "MMBT"
- Включить подробный вывод:
    ./mr-jira.py get issues <MR_URL> -v
"""

from __future__ import annotations

import os
import re
import sys
import logging
import urllib.parse
from dataclasses import dataclass
from typing import List, Optional, Sequence, Tuple, Dict, Any, Set

def _import_or_exit(module: str, pkg_hint: str) -> Any:
    try:
        return __import__(module)
    except Exception as e:
        print(
            f"Требуется пакет '{pkg_hint}'. Установите: pip install {pkg_hint}",
            file=sys.stderr,
        )
        raise


try:
    import typer  # type: ignore
except Exception:
    print("Требуется пакет 'typer[all]'. Установите: pip install typer[all]", file=sys.stderr)
    raise

try:
    from rich.console import Console  # type: ignore
except Exception:
    print("Требуется пакет 'rich'. Установите: pip install rich", file=sys.stderr)
    raise

try:
    import gitlab  # type: ignore
except Exception:
    print("Требуется пакет 'python-gitlab'. Установите: pip install python-gitlab", file=sys.stderr)
    raise

try:
    from jira import JIRA  # type: ignore
    from jira.exceptions import JIRAError  # type: ignore
except Exception:
    print("Требуется пакет 'jira'. Установите: pip install jira", file=sys.stderr)
    raise


app = typer.Typer(help="Инструменты для работы с GitLab MR и Jira")
get_app = typer.Typer(help="Команды получения данных (get)")
app.add_typer(get_app, name="get")

console = Console(stderr=False)
err_console = Console(stderr=True, style="bold red")


# ============================ Константы и настройки ============================

DEFAULT_JIRA_BASE = os.getenv("JIRA_BASE", "https://track.magnit.ru")
DEFAULT_JIRA_KEY_RE = os.getenv("JIRA_KEY_RE", r"[A-Z][A-Z0-9]+-[0-9]+")
DEFAULT_USER_AGENT = os.getenv(
    "USER_AGENT",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
)
DEFAULT_IGNORE_PATTERNS: Sequence[str] = (
    r"^Merge branch",
    r"^Merge remote-tracking branch",
    r"^Merge pull request",
    r"^Merge .* into ",
    r"^Revert ",
    r"^WIP",
    r"^\[skip ci\]",
)


# ============================ Вспомогательные структуры =========================

@dataclass(frozen=True)
class JiraRootIssue:
    key: str
    summary: str
    issuetype: str

    def as_url(self, jira_base: str) -> str:
        return f"{jira_base.rstrip('/')}/browse/{self.key}"

    def as_dict(self, jira_base: str) -> Dict[str, Any]:
        return {
            "key": self.key,
            "url": self.as_url(jira_base),
            "issuetype": self.issuetype,
            "summary": self.summary,
        }


# ============================ Утилиты ===========================================

def setup_logging(verbose: bool) -> None:
    level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(level=level, format="[%(levelname)s] %(message)s")


def parse_mr_url(mr_url: str) -> Tuple[str, str, str]:
    """Парсит URL MR формата https://HOST/<project_path>/-/merge_requests/<iid>

    Возвращает: (host, project_path, iid)
    """
    try:
        parsed = urllib.parse.urlparse(mr_url)
        host = parsed.netloc
        path = parsed.path
    except Exception:
        raise typer.BadParameter(f"Не удалось распарсить MR URL: {mr_url}")

    m = re.match(r"^/(.+)/-/merge_requests/(\d+)(?:/.*)?$", path)
    if not m or not host:
        raise typer.BadParameter(f"Ожидался формат 'https://HOST/<path>/-/merge_requests/<iid>': {mr_url}")
    project_path, iid = m.group(1), m.group(2)
    return host, project_path, iid


def compile_regexps(jira_key_re: str, ignore_patterns: Sequence[str]) -> Tuple[re.Pattern[str], List[re.Pattern[str]]]:
    try:
        key_rx = re.compile(jira_key_re)
    except re.error as e:
        raise typer.BadParameter(f"Некорректный regexp для Jira ключей: {e}")
    ignore_rx = []
    for p in ignore_patterns:
        try:
            ignore_rx.append(re.compile(p))
        except re.error as e:
            raise typer.BadParameter(f"Некорректный regexp в ignore-паттернах '{p}': {e}")
    return key_rx, ignore_rx


def is_ignored_commit(first_line: str, ignore_rx: Sequence[re.Pattern[str]]) -> bool:
    return any(rx.search(first_line) for rx in ignore_rx)


def extract_jira_keys_from_text(texts: Sequence[str], key_rx: re.Pattern[str]) -> Set[str]:
    keys: Set[str] = set()
    for t in texts:
        for k in key_rx.findall(t or ""):
            keys.add(k)
    return keys


def urlencode_path(p: str) -> str:
    return urllib.parse.quote(p, safe="")


# ============================ Клиенты GitLab/Jira ===============================

def build_gitlab_client(base_url: str, token: str, insecure: bool = False) -> Any:
    gl = gitlab.Gitlab(base_url, private_token=token, ssl_verify=not insecure)
    gl.auth()  # проверим токен
    return gl


def get_mr_commits(gl: Any, host: str, project_path: str, iid: str) -> List[Dict[str, Any]]:
    project = gl.projects.get(project_path)
    mr = project.mergerequests.get(iid)
    commits = mr.commits()
    return [
        {
            "title": getattr(c, "title", "") or "",
            "message": getattr(c, "message", "") or "",
        }
        for c in commits
    ]


def build_jira_client(
    jira_base: str,
    token: Optional[str],
    user: Optional[str],
    insecure: bool = False,
    user_agent: Optional[str] = None,
) -> JIRA:
    options: Dict[str, Any] = {
        "verify": not insecure,
    }
    headers: Dict[str, str] = {"Accept": "application/json"}
    if user_agent:
        headers["User-Agent"] = user_agent

    if token and not user:
        headers["Authorization"] = f"Bearer {token}"
        options["headers"] = headers
        return JIRA(server=jira_base, options=options)
    elif user and token:
        options["headers"] = headers
        return JIRA(server=jira_base, options=options, basic_auth=(user, token))
    else:
        raise typer.BadParameter("Не заданы учётные данные для Jira. Укажите --jira-token или --jira-user/--jira-token")


def resolve_root_issue(jira_client: JIRA, key: str) -> JiraRootIssue:
    current = key
    try:
        while True:
            issue = jira_client.issue(current, fields="summary,issuetype,parent")
            fields = issue.fields
            summary = (fields.summary or "").replace("\n", " ")
            issuetype = getattr(fields.issuetype, "name", "") or ""

            parent = getattr(fields, "parent", None)
            parent_key = getattr(parent, "key", None) if parent else None
            if not parent_key:
                return JiraRootIssue(current, summary, issuetype)
            current = parent_key
    except JIRAError as e:
        logging.warning("Jira error for %s: %s", current, e)
        return JiraRootIssue(current, "(unknown)", "(unknown)")
    except Exception as e:
        logging.warning("Unexpected Jira error for %s: %s", current, e)
        return JiraRootIssue(current, "(unknown)", "(unknown)")


# ============================ Форматы вывода ====================================

class OutputFormat(str):
    TEXT = "text"   # человекочитаемый текст
    MD = "md"       # markdown-список
    URLS = "urls"   # только ссылки
    JSON = "json"   # json-массив объектов


def render_output(
    issues: Sequence[JiraRootIssue],
    jira_base: str,
    fmt: str,
    mr_url: str,
) -> None:
    if fmt == OutputFormat.URLS:
        for i in sorted(issues, key=lambda x: x.key):
            console.print(i.as_url(jira_base))
        return

    if fmt == OutputFormat.JSON:
        payload = [i.as_dict(jira_base) for i in sorted(issues, key=lambda x: x.key)]
        console.print_json(data=payload)
        return

    # TEXT / MD — простой текстовый вывод, удобный для копирования в .md файл
    sorted_issues = sorted(issues, key=lambda x: x.key)
    print(f"Jira issues in MR:")
    print(f"MR - {mr_url}")
    print()
    for i in sorted_issues:
        print(f"- {i.as_url(jira_base)}  ({i.issuetype}) — {i.summary}")


# ============================ Команда: get issues ===============================


@get_app.command("issues")
def get_issues(
    mr_url: str = typer.Argument(..., help="Ссылка на MR в GitLab (https://host/group/proj/-/merge_requests/<iid>)"),
    # GitLab auth
    gitlab_token: Optional[str] = typer.Option(
        default=os.getenv("GITLAB_TOKEN"),
        help="Токен GitLab (env: GITLAB_TOKEN)",
        rich_help_panel="GitLab",
    ),
    gitlab_url_override: Optional[str] = typer.Option(
        default=None,
        help="База GitLab API/Host (по умолчанию берётся из MR-URL). Пример: https://gitlab.example.com",
        rich_help_panel="GitLab",
    ),
    # Jira auth
    jira_base: str = typer.Option(
        default=DEFAULT_JIRA_BASE,
        help=f"База Jira (env: JIRA_BASE) [по умолчанию: {DEFAULT_JIRA_BASE}]",
        rich_help_panel="Jira",
    ),
    jira_user: Optional[str] = typer.Option(
        default=os.getenv("JIRA_USER"), help="Пользователь Jira (если используется basic_auth)", rich_help_panel="Jira"
    ),
    jira_token: Optional[str] = typer.Option(
        default=os.getenv("JIRA_TOKEN"), help="API-токен Jira (env: JIRA_TOKEN)", rich_help_panel="Jira"
    ),
    user_agent: str = typer.Option(
        default=DEFAULT_USER_AGENT,
        help=f"User-Agent для запросов к Jira (env: USER_AGENT). По умолчанию браузерный UA.",
        rich_help_panel="Jira",
    ),
    # Поведение
    jira_key_re: str = typer.Option(
        default=DEFAULT_JIRA_KEY_RE,
        help=f"Regexp для Jira-ключей (env: JIRA_KEY_RE) [по умолчанию: {DEFAULT_JIRA_KEY_RE}]",
    ),
    ignore_pattern: List[str] = typer.Option(
        default=list(DEFAULT_IGNORE_PATTERNS),
        help="Regexp-паттерны для игнорирования коммитов по первой строке (можно указывать несколько)",
    ),
    jira_project: Optional[str] = typer.Option(
        default=None,
        help="Фильтр по проекту Jira (например MMBT). Если указан, в вывод попадут только issue этого проекта.",
        rich_help_panel="Jira",
    ),
    fmt: str = typer.Option(
        default=OutputFormat.MD,
        case_sensitive=False,
        help="Формат вывода: md|text|urls|json",
    ),
    insecure: bool = typer.Option(
        default=True,
        help="Игнорировать проверку SSL-сертификатов (как curl -k). По умолчанию включено; используйте --no-insecure для строгой проверки.",
    ),
    verbose: bool = typer.Option(False, "-v", "--verbose", help="Подробный вывод"),
) -> None:
    """Извлечь корневые Jira-задачи из коммитов Merge Request и вывести список задач.

    Логика соответствует shell-скрипту mr-jira.sh, но реализована на Python с удобным CLI.
    """
    setup_logging(verbose)

    if insecure:
        try:
            import urllib3  # type: ignore
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        except Exception:
            pass

    host, project_path, iid = parse_mr_url(mr_url)
    if not gitlab_token:
        raise typer.BadParameter("Не задан токен GitLab. Укажите --gitlab-token или env GITLAB_TOKEN")

    gitlab_base = gitlab_url_override or f"https://{host}"
    key_rx, ignore_rx = compile_regexps(jira_key_re, ignore_pattern)

    logging.debug("GitLab host: %s, project: %s, iid: %s", host, project_path, iid)
    # GitLab
    try:
        gl = build_gitlab_client(gitlab_base, gitlab_token, insecure=insecure)
    except Exception as e:
        err_console.print(f"Не удалось аутентифицироваться в GitLab: {e}")
        raise typer.Exit(code=2)

    # Получаем коммиты MR
    try:
        commits = get_mr_commits(gl, host, project_path, iid)
    except Exception as e:
        err_console.print(f"Ошибка при получении коммитов MR: {e}")
        raise typer.Exit(code=3)

    logging.info("Найдено коммитов в MR: %d", len(commits))

    # Извлекаем Jira-ключи
    found_keys: Set[str] = set()
    for c in commits:
        title = c.get("title") or ""
        message = c.get("message") or ""
        first_line = title or (message.splitlines()[0] if message else "")
        if is_ignored_commit(first_line, ignore_rx):
            logging.debug("Игнорируем коммит: %s", first_line)
            continue
        keys = extract_jira_keys_from_text([title, message], key_rx)
        if keys:
            logging.debug("Коммит: %s — ключи: %s", first_line, ", ".join(sorted(keys)))
        found_keys.update(keys)

    # Фильтрация по проекту Jira (если указан)
    if jira_project:
        project_prefix = jira_project.upper() + "-"
        filtered = {k for k in found_keys if k.startswith(project_prefix)}
        logging.info("Фильтр по проекту %s: %d из %d ключей", jira_project, len(filtered), len(found_keys))
        found_keys = filtered

    if not found_keys:
        console.print("No Jira issues found in commits for MR:")
        console.print(mr_url)
        raise typer.Exit(code=0)

    logging.info("Уникальные Jira-ключи: %d", len(found_keys))

    # Jira: резолвим корневые задачи
    try:
        jira_client = build_jira_client(
            jira_base,
            token=jira_token,
            user=jira_user,
            insecure=insecure,
            user_agent=user_agent,
        )
    except Exception as e:
        err_console.print(f"Не удалось подключиться к Jira: {e}")
        raise typer.Exit(code=4)

    root_map: Dict[str, JiraRootIssue] = {}
    for key in sorted(found_keys):
        root = resolve_root_issue(jira_client, key)
        # Сохраняем первую встреченную информацию о root key
        if root.key not in root_map:
            root_map[root.key] = root

    root_issues = list(root_map.values())
    render_output(root_issues, jira_base=jira_base, fmt=fmt.lower(), mr_url=mr_url)


# ============================ Точка входа =======================================


def main() -> None:
    try:
        app()
    except typer.Exit:
        # Нормальное завершение с заданным кодом
        raise
    except KeyboardInterrupt:
        err_console.print("Операция отменена пользователем")
        sys.exit(130)
    except Exception as e:
        err_console.print(f"Необработанная ошибка: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()