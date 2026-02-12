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
  pip install typer[all] python-gitlab jira rich

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
- Получить список задач (Markdown):
    ./mr-jira.py get issues "https://gitlab.example.com/group/proj/-/merge_requests/123"
- Вывести только URL-ы задач:
    ./mr-jira.py get issues <MR_URL> --format urls
- По умолчанию сертификаты TLS НЕ проверяются (как curl -k). Для строгой проверки используйте:
    ./mr-jira.py get issues <MR_URL> --no-insecure
- Включить подробный вывод:
    ./mr-jira.py get issues <MR_URL> -v
"""

from __future__ import annotations

import os
import re
import sys
import json
import logging
import urllib.parse
from dataclasses import dataclass
from typing import Iterable, List, Optional, Sequence, Tuple, Dict, Any, Set

# Ленивая загрузка внешних зависимостей с дружественными сообщениями об ошибках
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
    from rich.table import Table  # type: ignore
    from rich.markdown import Markdown  # type: ignore
except Exception:
    print("Требуется пакет 'rich'. Установите: pip install rich", file=sys.stderr)
    raise


# Популярные клиенты GitLab и Jira
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

    # Извлечение iid и project_path
    # path: /group/subgroup/project/-/merge_requests/623
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
    # В GitLab API id проекта может быть urlencoded path
    enc_path = urlencode_path(project_path)
    project = gl.projects.get(enc_path)
    mr = project.mergerequests.get(iid)
    # В python-gitlab метод .commits() возвращает все коммиты MR
    commits = mr.commits()
    # Коммиты представлены как dict-like объекты с .get
    return [dict(c) for c in commits]


def build_jira_client(
    jira_base: str,
    token: Optional[str],
    user: Optional[str],
    insecure: bool = False,
) -> JIRA:
    options = {"server": jira_base, "verify": not insecure}
    if user and token:
        # Базовая аутентификация (часто для DC/Server с PAT в качестве пароля)
        return JIRA(options=options, basic_auth=(user, token))
    elif token:
        # Token Auth (API Token)
        return JIRA(options=options, token_auth=token)
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

    if fmt == OutputFormat.MD:
        md_lines = [
            f"# Jira issues in MR:\n{mr_url}\n",
            f"Found {len(issues)} root issue(s):\n",
        ]
        for i in sorted(issues, key=lambda x: x.key):
            md_lines.append(f"- {i.as_url(jira_base)}  ({i.issuetype}) — {i.summary}")
        console.print(Markdown("\n".join(md_lines)))
        return

    # TEXT
    table = Table(title=f"Jira issues in MR: {mr_url}")
    table.add_column("Key", style="bold")
    table.add_column("Type")
    table.add_column("Summary")
    table.add_column("URL")
    for i in sorted(issues, key=lambda x: x.key):
        table.add_row(i.key, i.issuetype, i.summary, i.as_url(jira_base))
    console.print(table)


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
    # Поведение
    jira_key_re: str = typer.Option(
        default=DEFAULT_JIRA_KEY_RE,
        help=f"Regexp для Jira-ключей (env: JIRA_KEY_RE) [по умолчанию: {DEFAULT_JIRA_KEY_RE}]",
    ),
    ignore_pattern: List[str] = typer.Option(
        default=list(DEFAULT_IGNORE_PATTERNS),
        help="Regexp-паттерны для игнорирования коммитов по первой строке (можно указывать несколько)",
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

    # Подавляем предупреждения urllib3 об отключённой проверке сертификатов,
    # чтобы не засорять вывод при корпоративных сертификатах.
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

    if not found_keys:
        console.print("No Jira issues found in commits for MR:")
        console.print(mr_url)
        raise typer.Exit(code=0)

    logging.info("Уникальные Jira-ключи: %d", len(found_keys))

    # Jira: резолвим корневые задачи
    try:
        jira_client = build_jira_client(jira_base, token=jira_token, user=jira_user, insecure=insecure)
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