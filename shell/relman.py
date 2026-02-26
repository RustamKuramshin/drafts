#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
relman.py — CLI-инструмент для управления релизами в GitLab и Jira.

Ключевые возможности:
- Подключение к GitLab и Jira с аутентификацией по токенам/учётным данным.
- Парсинг MR-URL, получение коммитов MR и фильтрация «служебных» коммитов (merge/revert/wip/skip ci).
- Извлечение Jira-ключей из сообщений коммитов по регулярному выражению.
- Нахождение «корневых» задач в Jira (поднимаемся по полю parent у подзадач), дедупликация.
- Удобный CLI с иерархией команд: `get issues <MR_URL>` и богатыми опциями/справкой.

Требуемые библиотеки (установите при необходимости):

1) Только, если используется пакет anaconda:
conda create -n relman python=3.12
conda activate relman

2) Установка зависимостей:
python -m pip install python-gitlab
python -m pip install "typer[all]"
python -m pip install jira

Переменные окружения по умолчанию:
- GITLAB_TOKEN — токен для GitLab (private token или PAT)
- JIRA_TOKEN — токен для Jira (если не используется basic_auth)
- JIRA_BASE  — базовый URL Jira (по умолчанию https://track.magnit.ru)
- JIRA_KEY_RE — regexp для Jira-ключей (по умолчанию [A-Z][A-Z0-9]+-[0-9]+)
- USER_AGENT — User-Agent для Jira-запросов (при использовании прямого HTTP — не требуется с библиотеками)
"""

from __future__ import annotations

import os
import re
import sys
import logging
import base64
import pathlib
import urllib.parse
from dataclasses import dataclass
from datetime import date
from typing import List, Optional, Sequence, Tuple, Dict, Any, Set

import requests as _requests
import yaml

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


def _build_examples_epilog() -> str:
    # Важно: Typer 0.23 (rich_utils.rich_format_help) для epilog всегда:
    #   1) разбивает по "\n\n" на абзацы
    #   2) внутри каждого абзаца заменяет одиночные "\n" на пробелы и делает .strip()
    # Поэтому, чтобы не склеивались строки, каждая "строка" примера должна быть
    # отдельным абзацем (т.е. разделяться пустой строкой).
    lines = [
        "[bold]Примеры[/bold]",
        "",
        "[bold]Помощь[/bold]",
        "[dim]$[/dim] ./relman.py --help",
        "[dim]$[/dim] ./relman.py get issues --help",
        "",
        "[bold]Получить список задач[/bold]",
        "[dim]$[/dim] ./relman.py get issues https://gitlab.platform.corp/magnitonline/mm/backend/ke-backend/-/merge_requests/1808",
        "[dim]$[/dim] ./relman.py get issues https://gitlab.platform.corp/magnitonline/mm/backend/ke-backend/-/merge_requests/1808 --jira-project \"MMBT\"",
        "",
        "[bold]Создать релиз в Jira[/bold]",
        "[dim]$[/dim] ./relman.py create release https://gitlab.platform.corp/magnitonline/mm/backend/ke-backend/-/merge_requests/1808 --jira-project \"MMBT\" --gitlab-tag \"1.18.28\"",
        "[dim]$[/dim] ./relman.py create release https://gitlab.platform.corp/magnitonline/mm/backend/mm-core-bff/-/merge_requests/623 --jira-project \"MMBT\"",
        "",
        "[bold]Создать MR в GitLab с упоминанием Jira-задач из коммитов[/bold]",
        "[dim]$[/dim] ./relman.py create mr https://gitlab.platform.corp/magnitonline/mm/backend/api-graphql --from \"development\" --to \"stage\"",
        "",
        "[bold]Создать MR и Release в Jira[/bold]",
        "[dim]$[/dim] ./relman.py create mr https://gitlab.platform.corp/magnitonline/mm/backend/api-payment-service --from \"development\" --to \"stage\" --jira-project \"MMBT\" --with-release",
        "",
        "[bold]Batch-обработка создания MR для всех проектов из конфига[/bold]",
        "[dim]$[/dim] ./relman.py create mr --batch --target \"prod\"",
        "",
        "[bold]Dry-run для создания MR (без фактического создания MR)[/bold]",
        "[dim]$[/dim] ./relman.py create mr https://gitlab.platform.corp/magnitonline/mm/backend/api-graphql --from \"development\" --to \"stage\" --dry-run",
        "[dim]$[/dim] ./relman.py create mr --batch --target \"prod\" --dry-run",
        "",
        "[bold]Batch-обработка получения списка открытых MR для окружения[/bold]",
        "[dim]$[/dim] ./relman.py get mrs --batch --target \"prod\"",
        "[dim]$[/dim] ./relman.py get mrs --batch --target \"stage\"",
    ]
    return "\n\n".join(lines)


_EXAMPLES_EPILOG = _build_examples_epilog()

app = typer.Typer(
    help="CLI-инструмент для управления релизами в GitLab и Jira",
    epilog=_EXAMPLES_EPILOG,
    rich_markup_mode="rich",
    no_args_is_help=True,
)


# ============================ Конфигурация ======================================

# Глобальное хранилище загруженного конфига
_loaded_config: Dict[str, Any] = {}


def _find_config(explicit_path: Optional[str] = None) -> Optional[pathlib.Path]:
    """Ищет config.yaml по приоритету: --config > ./config.yaml > ~/.relman/config.yaml."""
    if explicit_path:
        p = pathlib.Path(explicit_path).expanduser()
        if p.is_file():
            return p
        return None

    # Рядом с relman.py
    script_dir = pathlib.Path(__file__).resolve().parent
    local = script_dir / "config.yaml"
    if local.is_file():
        return local

    # Текущий рабочий каталог
    cwd = pathlib.Path.cwd() / "config.yaml"
    if cwd.is_file():
        return cwd

    # ~/.relman/config.yaml
    home = pathlib.Path.home() / ".relman" / "config.yaml"
    if home.is_file():
        return home

    return None


def load_config(explicit_path: Optional[str] = None) -> Dict[str, Any]:
    """Загружает и возвращает конфиг. Кэширует в _loaded_config."""
    global _loaded_config
    if _loaded_config:
        return _loaded_config

    cfg_path = _find_config(explicit_path)
    if cfg_path is None:
        err_console.print(
            "[bold red]Файл config.yaml не найден![/bold red]\n"
            "Разместите его в одном из мест:\n"
            "  1) рядом с relman.py\n"
            "  2) в текущем каталоге\n"
            "  3) ~/.relman/config.yaml\n"
            "или укажите путь через --config <path>"
        )
        raise typer.Exit(code=1)

    with open(cfg_path, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f) or {}

    logging.debug("Конфиг загружен из %s", cfg_path)
    _loaded_config = data
    return _loaded_config


def cfg_defaults(cfg: Dict[str, Any]) -> Dict[str, Any]:
    """Возвращает раздел defaults из конфига."""
    return cfg.get("defaults", {})


def cfg_gitlab(cfg: Dict[str, Any]) -> Dict[str, Any]:
    return cfg_defaults(cfg).get("gitlab", {})


def cfg_jira(cfg: Dict[str, Any]) -> Dict[str, Any]:
    return cfg_defaults(cfg).get("jira", {})


def cfg_commit_filter(cfg: Dict[str, Any]) -> Dict[str, Any]:
    return cfg_defaults(cfg).get("commit_filter", {})


def cfg_projects(cfg: Dict[str, Any]) -> List[Dict[str, Any]]:
    return cfg.get("projects", [])

@app.callback(invoke_without_command=True)
def app_callback(
    ctx: typer.Context,
    show_help: bool = typer.Option(False, "--help", "-h", help="Показать справку и выйти."),
    config: Optional[str] = typer.Option(
        None, "--config", help="Путь к файлу config.yaml",
    ),
):
    """
    Инструмент позволяет извлекать Jira-задачи из коммитов GitLab MR,
    автоматически создавать релизы в Jira и новые Merge Request'ы.

    Используйте подкоманды для конкретных действий:
    [bold]get[/bold], [bold]create[/bold].
    """
    if show_help or ctx.invoked_subcommand is None:
        console.print(ctx.get_help())
        raise typer.Exit()

    # Загружаем конфиг (обязателен для всех команд, кроме help)
    ctx.ensure_object(dict)
    ctx.obj["config"] = load_config(config)


@app.command("help", hidden=True)
def custom_help(ctx: typer.Context):
    """Показать полную справку по утилите."""
    console.print(ctx.parent.get_help() if ctx.parent else ctx.get_help())
    raise typer.Exit()


get_app = typer.Typer(
    help="Команды получения данных (get issues)",
    no_args_is_help=True,
)
app.add_typer(get_app, name="get")

create_app = typer.Typer(
    help="Команды создания (create release, create mr)",
    no_args_is_help=True,
)
app.add_typer(create_app, name="create")

console = Console(stderr=False)
err_console = Console(stderr=True, style="bold red")


# ============================ Константы и настройки ============================

# Fallback-значения (используются, если в config.yaml что-то не указано)
_FALLBACK_JIRA_BASE = "https://track.magnit.ru"
_FALLBACK_JIRA_KEY_RE = r"[A-Z][A-Z0-9]+-[0-9]+"
_FALLBACK_USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
)
_FALLBACK_IGNORE_PATTERNS: Sequence[str] = (
    r"^Merge branch",
    r"^Merge remote-tracking branch",
    r"^Merge pull request",
    r"^Merge .* into ",
    r"^Revert ",
    r"^WIP",
    r"^\[skip ci\]",
)


def _resolve_defaults(cfg: Dict[str, Any]) -> Dict[str, Any]:
    """Возвращает разрешённые значения по умолчанию из конфига + env + fallback."""
    gl = cfg_gitlab(cfg)
    ji = cfg_jira(cfg)
    cf = cfg_commit_filter(cfg)

    gitlab_token_env = gl.get("token_env", "GITLAB_TOKEN")
    jira_token_env = ji.get("token_env", "JIRA_TOKEN")
    jira_user_env = ji.get("user_env", "JIRA_USER")

    return {
        "gitlab_base_url": gl.get("base_url", ""),
        "gitlab_token": os.getenv(gitlab_token_env, ""),
        "gitlab_insecure": gl.get("insecure", True),
        "jira_base": ji.get("base_url") or os.getenv("JIRA_BASE", _FALLBACK_JIRA_BASE),
        "jira_token": os.getenv(jira_token_env, ""),
        "jira_user": os.getenv(jira_user_env, ""),
        "jira_project": ji.get("project", ""),
        "jira_key_re": ji.get("key_re") or os.getenv("JIRA_KEY_RE", _FALLBACK_JIRA_KEY_RE),
        "jira_insecure": ji.get("insecure", True),
        "user_agent": ji.get("user_agent") or os.getenv("USER_AGENT", _FALLBACK_USER_AGENT),
        "ignore_patterns": cf.get("ignore_patterns", list(_FALLBACK_IGNORE_PATTERNS)),
    }


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


def parse_repo_url(repo_url: str) -> Tuple[str, str]:
    """Парсит URL репозитория формата https://HOST/<project_path>

    Возвращает: (host, project_path)
    """
    try:
        parsed = urllib.parse.urlparse(repo_url)
        host = parsed.netloc
        path = parsed.path.rstrip("/")
    except Exception:
        raise typer.BadParameter(f"Не удалось распарсить URL репозитория: {repo_url}")

    if not host or not path or path == "/":
        raise typer.BadParameter(f"Ожидался формат 'https://HOST/<project_path>': {repo_url}")

    # Убираем ведущий слэш
    project_path = path.lstrip("/")
    return host, project_path


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


def get_project_tags(gl: Any, project_path: str) -> List[str]:
    """Получает список тэгов проекта в GitLab и возвращает их имена."""
    project = gl.projects.get(project_path)
    tags = project.tags.list(all=True)
    return [t.name for t in tags]


def parse_semver(tag: str) -> Optional[Tuple[int, int, int]]:
    """Парсит semver-тэг (с опциональным префиксом 'v'). Возвращает (major, minor, patch) или None."""
    m = re.match(r"^v?(\d+)\.(\d+)\.(\d+)$", tag)
    if not m:
        return None
    return int(m.group(1)), int(m.group(2)), int(m.group(3))


def compute_next_tag(tags: List[str]) -> str:
    """Вычисляет следующий тэг по semver (инкремент minor-версии).

    Из списка тэгов выбирается максимальный по semver, затем увеличивается minor и patch сбрасывается в 0.
    """
    semver_tags: List[Tuple[int, int, int]] = []
    for t in tags:
        sv = parse_semver(t)
        if sv:
            semver_tags.append(sv)
    if not semver_tags:
        raise typer.BadParameter("Не найдено semver-тэгов в проекте GitLab. Укажите тэг явно через --gitlab-tag.")
    semver_tags.sort()
    latest = semver_tags[-1]
    next_ver = (latest[0], latest[1] + 1, 0)
    return f"{next_ver[0]}.{next_ver[1]}.{next_ver[2]}"


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
    headers: Dict[str, str] = {
        "Accept": "application/json",
        "X-Atlassian-Token": "no-check",
    }
    if user_agent:
        headers["User-Agent"] = user_agent

    if token and not user:
        logging.info("Use Bearer token")
        headers["Authorization"] = f"Bearer {token}"
        options["headers"] = headers
        return JIRA(server=jira_base, options=options)
    elif user and token:
        logging.info("Use basic auth")
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
    project_name: str = "",
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
    print(project_name)
    print(f"MR - {mr_url}")
    print()
    for idx, i in enumerate(sorted_issues):
        if idx > 0:
            print()
        print(f"- ({i.issuetype}) {i.summary}")
        print(f"  {i.as_url(jira_base)}")


# ============================ Общая логика: извлечение issue из MR ===============


def extract_jira_keys_from_commits(
    commits: List[Dict[str, str]],
    key_rx: re.Pattern[str],
    ignore_rx: List[re.Pattern[str]],
    jira_project: Optional[str] = None,
) -> Set[str]:
    """Извлекает уникальные Jira-ключи из списка коммитов с фильтрацией."""
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

    return found_keys


def extract_issues_from_mr_id(
    gl: Any,
    project_path: str,
    iid: str,
    jira_base: str,
    jira_user: Optional[str],
    jira_token: Optional[str],
    user_agent: str,
    jira_key_re: str,
    ignore_pattern: List[str],
    jira_project: Optional[str],
    insecure: bool,
    mr_url_for_log: str,
) -> List[JiraRootIssue]:
    """Извлекает корневые Jira-issue из коммитов MR по его IID."""
    key_rx, ignore_rx = compile_regexps(jira_key_re, ignore_pattern)

    # Получаем коммиты MR
    try:
        # Для логов нам нужен host, но в get_mr_commits он не используется для запроса
        project = gl.projects.get(project_path)
        mr = project.mergerequests.get(iid)
        commits = [
            {
                "title": getattr(c, "title", "") or "",
                "message": getattr(c, "message", "") or "",
            }
            for c in mr.commits()
        ]
    except Exception as e:
        err_console.print(f"Ошибка при получении коммитов MR {iid}: {e}")
        raise typer.Exit(code=3)

    logging.info("Найдено коммитов в MR: %d", len(commits))

    # Извлекаем Jira-ключи
    found_keys = extract_jira_keys_from_commits(
        commits=commits,
        key_rx=key_rx,
        ignore_rx=ignore_rx,
        jira_project=jira_project
    )

    if not found_keys:
        console.print("No Jira issues found in commits for MR:")
        console.print(mr_url_for_log)
        return []

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
        if root.key not in root_map:
            root_map[root.key] = root

    return list(root_map.values())


def extract_issues_from_mr(
    mr_url: str,
    gitlab_token: str,
    gitlab_url_override: Optional[str],
    jira_base: str,
    jira_user: Optional[str],
    jira_token: Optional[str],
    user_agent: str,
    jira_key_re: str,
    ignore_pattern: List[str],
    jira_project: Optional[str],
    insecure: bool,
) -> Tuple[str, str, List[JiraRootIssue], JIRA, Any]:
    """Извлекает корневые Jira-issue из коммитов MR.

    Возвращает: (project_path, project_name, root_issues, jira_client, gl)
    """
    host, project_path, iid = parse_mr_url(mr_url)
    if not gitlab_token:
        raise typer.BadParameter("Не задан токен GitLab. Укажите --gitlab-token или env GITLAB_TOKEN")

    gitlab_base = gitlab_url_override or f"https://{host}"

    logging.debug("GitLab host: %s, project: %s, iid: %s", host, project_path, iid)
    # GitLab
    try:
        gl = build_gitlab_client(gitlab_base, gitlab_token, insecure=insecure)
    except Exception as e:
        err_console.print(f"Не удалось аутентифицироваться в GitLab: {e}")
        raise typer.Exit(code=2)

    root_issues = extract_issues_from_mr_id(
        gl=gl,
        project_path=project_path,
        iid=iid,
        jira_base=jira_base,
        jira_user=jira_user,
        jira_token=jira_token,
        user_agent=user_agent,
        jira_key_re=jira_key_re,
        ignore_pattern=ignore_pattern,
        jira_project=jira_project,
        insecure=insecure,
        mr_url_for_log=mr_url,
    )

    if not root_issues:
        raise typer.Exit(code=0)

    # Jira client (re-build for returning if needed, though extract_issues_from_mr_id builds it too)
    jira_client = build_jira_client(
        jira_base,
        token=jira_token,
        user=jira_user,
        insecure=insecure,
        user_agent=user_agent,
    )

    project_name = project_path.rsplit("/", 1)[-1]
    return project_path, project_name, root_issues, jira_client, gl


# ============================ Команда: get mrs (batch) =========================


@dataclass(frozen=True)
class OpenMr:
    project_id: str
    project_path: str
    iid: int
    title: str
    web_url: str
    source_branch: str
    target_branch: str


def _mr_matches_env(
    *,
    source_branch: str,
    target_branch: str,
    env_from: str,
    env_to: str,
    is_release_env: bool,
) -> bool:
    if target_branch != env_to:
        return False
    if not is_release_env:
        return source_branch == env_from
    # Для релизного окружения (обычно prod): допускаем release/* ветки,
    # а также классическую пару from->to из конфига.
    return source_branch == env_from or source_branch.startswith("release/")


def _resolve_root_issue_cached(
    jira_client: JIRA,
    key: str,
    cache: Dict[str, JiraRootIssue],
) -> JiraRootIssue:
    cached = cache.get(key)
    if cached:
        return cached
    root = resolve_root_issue(jira_client, key)
    cache[key] = root
    cache[root.key] = root
    return root


def extract_root_issues_from_commits_cached(
    *,
    commits: List[Dict[str, str]],
    key_rx: re.Pattern[str],
    ignore_rx: List[re.Pattern[str]],
    jira_client: JIRA,
    jira_project: Optional[str],
    cache: Dict[str, JiraRootIssue],
) -> List[JiraRootIssue]:
    found_keys = extract_jira_keys_from_commits(
        commits=commits,
        key_rx=key_rx,
        ignore_rx=ignore_rx,
        jira_project=jira_project,
    )
    if not found_keys:
        return []

    root_map: Dict[str, JiraRootIssue] = {}
    for key in sorted(found_keys):
        root = _resolve_root_issue_cached(jira_client, key, cache)
        root_map[root.key] = root

    return list(root_map.values())


def _list_open_mrs_for_env(
    *,
    project: Any,
    project_id: str,
    project_path: str,
    env_from: str,
    env_to: str,
    target_name: Optional[str],
) -> List[OpenMr]:
    is_release_env = _is_release_mr(target_name, env_to)

    try:
        if is_release_env:
            # Нельзя запросить "source_branch startswith release/" через API,
            # поэтому берём все открытые MR в целевую ветку и фильтруем локально.
            candidates = project.mergerequests.list(state="opened", target_branch=env_to, all=True)
        else:
            candidates = project.mergerequests.list(
                state="opened",
                source_branch=env_from,
                target_branch=env_to,
                all=True,
            )
    except Exception as e:
        err_console.print(f"Ошибка при получении списка MR для {project_id}: {e}")
        return []

    result: List[OpenMr] = []
    for mr in candidates:
        iid = int(getattr(mr, "iid", 0) or 0)
        if not iid:
            continue

        source_branch = getattr(mr, "source_branch", "") or ""
        target_branch = getattr(mr, "target_branch", "") or ""

        # Иногда list() возвращает урезанный объект без веток — добираем.
        if not source_branch or not target_branch:
            try:
                mr_full = project.mergerequests.get(iid)
                source_branch = getattr(mr_full, "source_branch", "") or source_branch
                target_branch = getattr(mr_full, "target_branch", "") or target_branch
                mr = mr_full
            except Exception:
                pass

        if not _mr_matches_env(
            source_branch=source_branch,
            target_branch=target_branch,
            env_from=env_from,
            env_to=env_to,
            is_release_env=is_release_env,
        ):
            continue

        result.append(
            OpenMr(
                project_id=project_id,
                project_path=project_path,
                iid=iid,
                title=getattr(mr, "title", "") or "",
                web_url=getattr(mr, "web_url", "") or "",
                source_branch=source_branch,
                target_branch=target_branch,
            )
        )

    result.sort(key=lambda x: x.iid)
    return result


# ============================ Логика создания релиза ============================


def execute_create_release(
    gl: Any,
    project_path: str,
    project_name: str,
    root_issues: List[JiraRootIssue],
    jira_client: JIRA,
    jira_base: str,
    jira_token: Optional[str],
    jira_user: Optional[str],
    jira_project: str,
    gitlab_tag: Optional[str],
    user_agent: str,
    insecure: bool,
) -> None:
    """Выполняет создание релиза в Jira и привязку к нему задач."""
    # Определяем тэг
    tag = gitlab_tag
    if not tag:
        logging.info("Тэг не указан, вычисляем следующий из тэгов проекта GitLab...")
        try:
            tags = get_project_tags(gl, project_path)
        except Exception as e:
            err_console.print(f"Ошибка при получении тэгов GitLab: {e}")
            raise typer.Exit(code=5)
        tag = compute_next_tag(tags)
        logging.info("Вычисленный следующий тэг: %s", tag)

    version_name = f"{project_name}:{tag}"
    start_date = date.today().isoformat()
    description = "Создан автоматически"

    # Создаём релиз (версию) в Jira через прямой REST-вызов
    logging.info("Создаём релиз в Jira: %s (проект: %s)", version_name, jira_project)
    try:
        jira_origin = jira_base.rstrip('/')

        # Решаем вопрос с авторизацией для прямых запросов
        if jira_user and jira_token:
            auth_str = f"{jira_user}:{jira_token}"
            encoded_auth = base64.b64encode(auth_str.encode()).decode()
            auth_header = f"Basic {encoded_auth}"
        else:
            auth_header = f"Bearer {jira_token}"

        mutating_headers = {
            "Authorization": auth_header,
            "Accept": "application/json",
            "Content-Type": "application/json",
            "X-Atlassian-Token": "no-check",
            "X-Requested-With": "XMLHttpRequest",
            "Origin": jira_origin,
            "User-Agent": user_agent,
        }

        _create_payload = {
            "name": version_name,
            "project": jira_project,
            "description": description,
            "startDate": start_date,
            "archived": False,
            "released": False,
        }

        _resp = _requests.post(
            f"{jira_origin}/rest/api/2/version",
            headers=mutating_headers,
            json=_create_payload,
            verify=not insecure,
        )
        _resp.raise_for_status()
    except _requests.HTTPError as e:
        err_console.print(f"Ошибка при создании релиза в Jira: {e}")
        if hasattr(e, 'response') and e.response is not None:
            err_console.print(f"Response: {e.response.text}")
        raise typer.Exit(code=6)
    except Exception as e:
        err_console.print(f"Ошибка при создании релиза в Jira: {e}")
        raise typer.Exit(code=6)

    print(f"Создан релиз: {version_name}")
    print(f"  Проект Jira: {jira_project}")
    print(f"  Дата начала: {start_date}")
    print(f"  Описание: {description}")
    print()

    # Привязываем issue к релизу (устанавливаем fixVersions) через прямые REST-вызовы
    for issue in sorted(root_issues, key=lambda x: x.key):
        try:
            jira_issue = jira_client.issue(issue.key, fields="fixVersions")
            existing = [v.name for v in jira_issue.fields.fixVersions] if jira_issue.fields.fixVersions else []
            if version_name not in existing:
                existing.append(version_name)
                _update_payload = {"fields": {"fixVersions": [{"name": n} for n in existing]}}
                _resp = _requests.put(
                    f"{jira_origin}/rest/api/2/issue/{issue.key}",
                    headers=mutating_headers,
                    json=_update_payload,
                    verify=not insecure,
                )
                _resp.raise_for_status()
            print(f"- {issue.key}: fixVersion установлен → {version_name}")
        except _requests.HTTPError as e:
            err_console.print(f"Ошибка при обновлении {issue.key}: {e}")
            if hasattr(e, 'response') and e.response is not None:
                err_console.print(f"Response: {e.response.text}")
        except JIRAError as e:
            err_console.print(f"Ошибка при обновлении {issue.key}: {e}")

    print()
    print(f"Готово. В релиз {version_name} включено {len(root_issues)} issue.")


# ============================ Команда: get issues ===============================


@get_app.command("issues")
def get_issues(
    ctx: typer.Context,
    mr_url: str = typer.Argument(..., help="Ссылка на MR в GitLab (https://host/group/proj/-/merge_requests/<iid>)"),
    # GitLab auth
    gitlab_token: Optional[str] = typer.Option(
        default=None,
        help="Токен GitLab (env: GITLAB_TOKEN)",
        rich_help_panel="GitLab",
    ),
    gitlab_url_override: Optional[str] = typer.Option(
        default=None,
        help="База GitLab API/Host (по умолчанию берётся из MR-URL). Пример: https://gitlab.example.com",
        rich_help_panel="GitLab",
    ),
    # Jira auth
    jira_base: Optional[str] = typer.Option(
        default=None,
        help="База Jira (из config.yaml или env JIRA_BASE)",
        rich_help_panel="Jira",
    ),
    jira_user: Optional[str] = typer.Option(
        default=None, help="Пользователь Jira (если используется basic_auth)", rich_help_panel="Jira"
    ),
    jira_token: Optional[str] = typer.Option(
        default=None, help="API-токен Jira (env: JIRA_TOKEN)", rich_help_panel="Jira"
    ),
    user_agent: Optional[str] = typer.Option(
        default=None,
        help="User-Agent для запросов к Jira.",
        rich_help_panel="Jira",
    ),
    # Поведение
    jira_key_re: Optional[str] = typer.Option(
        default=None,
        help="Regexp для Jira-ключей",
    ),
    ignore_pattern: Optional[List[str]] = typer.Option(
        default=None,
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
    insecure: Optional[bool] = typer.Option(
        default=None,
        help="Игнорировать проверку SSL-сертификатов (как curl -k).",
    ),
    verbose: bool = typer.Option(False, "-v", "--verbose", help="Подробный вывод"),
) -> None:
    """Извлечь корневые Jira-задачи из коммитов Merge Request и вывести список задач.
    """
    setup_logging(verbose)

    cfg = ctx.obj["config"]
    d = _resolve_defaults(cfg)

    gitlab_token = gitlab_token or d["gitlab_token"]
    jira_base = jira_base or d["jira_base"]
    jira_user = jira_user or d["jira_user"] or None
    jira_token = jira_token or d["jira_token"] or None
    user_agent = user_agent or d["user_agent"]
    jira_key_re = jira_key_re or d["jira_key_re"]
    ignore_pattern = ignore_pattern if ignore_pattern is not None else d["ignore_patterns"]
    jira_project = jira_project or d["jira_project"] or None
    insecure = insecure if insecure is not None else d["gitlab_insecure"]

    if insecure:
        try:
            import urllib3  # type: ignore
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        except Exception:
            pass

    project_path, project_name, root_issues, jira_client, gl = extract_issues_from_mr(
        mr_url=mr_url,
        gitlab_token=gitlab_token,
        gitlab_url_override=gitlab_url_override,
        jira_base=jira_base,
        jira_user=jira_user,
        jira_token=jira_token,
        user_agent=user_agent,
        jira_key_re=jira_key_re,
        ignore_pattern=ignore_pattern,
        jira_project=jira_project,
        insecure=insecure,
    )
    render_output(root_issues, jira_base=jira_base, fmt=fmt.lower(), mr_url=mr_url, project_name=project_name)


@get_app.command("mrs")
def get_mrs(
    ctx: typer.Context,
    # Batch-режим
    batch: bool = typer.Option(False, "--batch", help="Batch-режим: перебрать все проекты из config.yaml"),
    target: Optional[str] = typer.Option(None, "--target", help="Имя target из config.yaml (например stage, prod)."),
    # GitLab auth
    gitlab_token: Optional[str] = typer.Option(
        default=None,
        help="Токен GitLab (env: GITLAB_TOKEN)",
        rich_help_panel="GitLab",
    ),
    gitlab_url_override: Optional[str] = typer.Option(
        default=None,
        help="База GitLab API/Host (по умолчанию берётся из repo_url).",
        rich_help_panel="GitLab",
    ),
    # Jira auth
    jira_base: Optional[str] = typer.Option(
        default=None,
        help="База Jira (из config.yaml или env JIRA_BASE)",
        rich_help_panel="Jira",
    ),
    jira_user: Optional[str] = typer.Option(
        default=None, help="Пользователь Jira (если используется basic_auth)", rich_help_panel="Jira"
    ),
    jira_token: Optional[str] = typer.Option(
        default=None, help="API-токен Jira (env: JIRA_TOKEN)", rich_help_panel="Jira"
    ),
    user_agent: Optional[str] = typer.Option(
        default=None,
        help="User-Agent для запросов к Jira.",
        rich_help_panel="Jira",
    ),
    # Поведение
    jira_project: Optional[str] = typer.Option(
        default=None,
        help="Фильтр по проекту Jira (например MMBT). Если указан, в вывод попадут только issue этого проекта.",
        rich_help_panel="Jira",
    ),
    jira_key_re: Optional[str] = typer.Option(
        default=None,
        help="Regexp для Jira-ключей",
    ),
    ignore_pattern: Optional[List[str]] = typer.Option(
        default=None,
        help="Regexp-паттерны для игнорирования коммитов по первой строке",
    ),
    fmt: str = typer.Option(
        default=OutputFormat.MD,
        case_sensitive=False,
        help="Формат вывода: md|text|urls|json",
    ),
    insecure: Optional[bool] = typer.Option(
        default=None,
        help="Игнорировать проверку SSL-сертификатов (как curl -k).",
    ),
    verbose: bool = typer.Option(False, "-v", "--verbose", help="Подробный вывод"),
) -> None:
    """Получить список открытых (не смерженных) MR для конкретного окружения в batch-режиме.

    Команда перебирает проекты из config.yaml, находит открытые MR для пары веток target-а
    (from→to) и для каждого MR печатает Jira-задачи, найденные в коммитах (аналогично `get issues`).
    В конце печатается сводный список всех найденных MR.
    """
    setup_logging(verbose)

    if not batch:
        raise typer.BadParameter("Команда поддерживает только batch-режим. Укажите --batch.")
    if not target:
        raise typer.BadParameter("В batch-режиме необходимо указать --target (например --target stage)")

    cfg = ctx.obj["config"]
    d = _resolve_defaults(cfg)

    gitlab_token = gitlab_token or d["gitlab_token"]
    jira_base = jira_base or d["jira_base"]
    jira_user = jira_user or d["jira_user"] or None
    jira_token = jira_token or d["jira_token"] or None
    user_agent = user_agent or d["user_agent"]
    jira_key_re = jira_key_re or d["jira_key_re"]
    ignore_pattern = ignore_pattern if ignore_pattern is not None else d["ignore_patterns"]
    jira_project = jira_project or d["jira_project"] or None
    insecure = insecure if insecure is not None else d["gitlab_insecure"]

    if not gitlab_token:
        raise typer.BadParameter("Не задан токен GitLab. Укажите --gitlab-token или env GITLAB_TOKEN")

    if insecure:
        try:
            import urllib3  # type: ignore

            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        except Exception:
            pass

    projects = cfg_projects(cfg)
    if not projects:
        err_console.print("В config.yaml не определены проекты (projects).")
        raise typer.Exit(code=1)

    projects_sorted = sorted(projects, key=lambda p: p.get("deploy", {}).get("order", 999))

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

    key_rx, ignore_rx = compile_regexps(jira_key_re, ignore_pattern)
    root_cache: Dict[str, JiraRootIssue] = {}
    gl_cache: Dict[str, Any] = {}

    all_mrs: List[OpenMr] = []

    for proj in projects_sorted:
        proj_id = proj.get("id", proj.get("name", "unknown"))
        targets = proj.get("targets", {})
        if target not in targets:
            console.print(f"[dim]⏭  {proj_id}: target '{target}' не определён, пропускаем.[/dim]")
            continue

        repo_url = proj.get("repo_url", "")
        if not repo_url:
            err_console.print(f"Проект {proj_id}: не указан repo_url, пропускаем.")
            continue

        t = targets[target]
        env_from = t.get("from", "")
        env_to = t.get("to", "")
        if not env_from or not env_to:
            err_console.print(f"Проект {proj_id}: target '{target}' не содержит from/to, пропускаем.")
            continue

        host, project_path = parse_repo_url(repo_url)
        gitlab_base = gitlab_url_override or f"https://{host}"

        gl = gl_cache.get(gitlab_base)
        if gl is None:
            try:
                gl = build_gitlab_client(gitlab_base, gitlab_token, insecure=insecure)
            except Exception as e:
                err_console.print(f"Не удалось аутентифицироваться в GitLab ({gitlab_base}): {e}")
                continue
            gl_cache[gitlab_base] = gl

        try:
            project = gl.projects.get(project_path)
        except Exception as e:
            err_console.print(f"Ошибка при получении проекта GitLab {project_path}: {e}")
            continue

        open_mrs = _list_open_mrs_for_env(
            project=project,
            project_id=str(proj_id),
            project_path=project_path,
            env_from=env_from,
            env_to=env_to,
            target_name=target,
        )

        if not open_mrs:
            console.print(f"[dim]— {proj_id}: открытых MR для '{env_from}' → '{env_to}' не найдено.[/dim]")
            continue

        console.print(f"\n[bold]{'=' * 60}[/bold]")
        console.print(f"[bold]📦 Проект: {proj_id}[/bold]  ({env_from} → {env_to})")
        console.print(f"[bold]{'=' * 60}[/bold]")

        for mr_info in open_mrs:
            # Коммиты MR
            try:
                mr_obj = project.mergerequests.get(mr_info.iid)
                commits = [
                    {
                        "title": getattr(c, "title", "") or "",
                        "message": getattr(c, "message", "") or "",
                    }
                    for c in mr_obj.commits()
                ]
            except Exception as e:
                err_console.print(f"Ошибка при получении коммитов MR {proj_id}!{mr_info.iid}: {e}")
                continue

            root_issues = extract_root_issues_from_commits_cached(
                commits=commits,
                key_rx=key_rx,
                ignore_rx=ignore_rx,
                jira_client=jira_client,
                jira_project=jira_project,
                cache=root_cache,
            )

            # Детальный вывод как в get issues
            mr_title = mr_info.title or f"MR !{mr_info.iid}"
            project_title = f"{proj_id}: {mr_title}  ({mr_info.source_branch} → {mr_info.target_branch})"
            if root_issues:
                render_output(
                    root_issues,
                    jira_base=jira_base,
                    fmt=fmt.lower(),
                    mr_url=mr_info.web_url,
                    project_name=project_title,
                )
            else:
                print(project_title)
                print(f"MR - {mr_info.web_url}")
                print()
                print("- (Jira задачи не найдены в коммитах этого MR)")

            all_mrs.append(mr_info)

    console.print(f"\n[bold]{'=' * 60}[/bold]")
    console.print(f"[bold]Сводка: открытые MR для target '{target}'[/bold]")
    console.print(f"[bold]{'=' * 60}[/bold]")

    if not all_mrs:
        console.print("[yellow]Открытые MR не найдены.[/yellow]")
        raise typer.Exit(code=0)

    console.print(f"Найдено MR: [bold]{len(all_mrs)}[/bold]")
    for mr in all_mrs:
        console.print(f"- {mr.project_id}: {mr.web_url}")


# ============================ Команда: create release ===========================


@create_app.command("release")
def create_release(
    ctx: typer.Context,
    mr_url: str = typer.Argument(..., help="Ссылка на MR в GitLab (https://host/group/proj/-/merge_requests/<iid>)"),
    # GitLab auth
    gitlab_token: Optional[str] = typer.Option(
        default=None,
        help="Токен GitLab (env: GITLAB_TOKEN)",
        rich_help_panel="GitLab",
    ),
    gitlab_url_override: Optional[str] = typer.Option(
        default=None,
        help="База GitLab API/Host (по умолчанию берётся из MR-URL).",
        rich_help_panel="GitLab",
    ),
    gitlab_tag: Optional[str] = typer.Option(
        default=None,
        help="Следующий тэг (semver) для названия релиза. Если не указан — вычисляется автоматически из тэгов проекта в GitLab.",
        rich_help_panel="GitLab",
    ),
    # Jira auth
    jira_base: Optional[str] = typer.Option(
        default=None,
        help="База Jira (из config.yaml или env JIRA_BASE)",
        rich_help_panel="Jira",
    ),
    jira_user: Optional[str] = typer.Option(
        default=None, help="Пользователь Jira (если используется basic_auth)", rich_help_panel="Jira"
    ),
    jira_token: Optional[str] = typer.Option(
        default=None, help="API-токен Jira (env: JIRA_TOKEN)", rich_help_panel="Jira"
    ),
    user_agent: Optional[str] = typer.Option(
        default=None,
        help="User-Agent для запросов к Jira.",
        rich_help_panel="Jira",
    ),
    # Jira project — обязательный для create release (берётся из конфига, если не указан)
    jira_project: Optional[str] = typer.Option(
        default=None,
        help="Проект Jira (например MMBT). Обязателен для создания релиза.",
        rich_help_panel="Jira",
    ),
    # Поведение
    jira_key_re: Optional[str] = typer.Option(
        default=None,
        help="Regexp для Jira-ключей",
    ),
    ignore_pattern: Optional[List[str]] = typer.Option(
        default=None,
        help="Regexp-паттерны для игнорирования коммитов по первой строке",
    ),
    insecure: Optional[bool] = typer.Option(
        default=None,
        help="Игнорировать проверку SSL-сертификатов (как curl -k).",
    ),
    verbose: bool = typer.Option(False, "-v", "--verbose", help="Подробный вывод"),
) -> None:
    """Создать релиз (версию) в Jira и включить в него найденные issue из MR.

    Название релиза: <имя_проекта_GitLab>:<тэг> (например ke-backend:1.28.0).
    Если --gitlab-tag не указан, следующий тэг вычисляется автоматически из тэгов проекта.
    """
    setup_logging(verbose)

    cfg = ctx.obj["config"]
    d = _resolve_defaults(cfg)

    gitlab_token = gitlab_token or d["gitlab_token"]
    jira_base = jira_base or d["jira_base"]
    jira_user = jira_user or d["jira_user"] or None
    jira_token = jira_token or d["jira_token"] or None
    user_agent = user_agent or d["user_agent"]
    jira_key_re = jira_key_re or d["jira_key_re"]
    ignore_pattern = ignore_pattern if ignore_pattern is not None else d["ignore_patterns"]
    jira_project = jira_project or d["jira_project"] or None
    insecure = insecure if insecure is not None else d["gitlab_insecure"]

    if not jira_project:
        raise typer.BadParameter("Не задан проект Jira. Укажите --jira-project или задайте в config.yaml (defaults.jira.project)")

    if insecure:
        try:
            import urllib3  # type: ignore
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        except Exception:
            pass

    project_path, project_name, root_issues, jira_client, gl = extract_issues_from_mr(
        mr_url=mr_url,
        gitlab_token=gitlab_token,
        gitlab_url_override=gitlab_url_override,
        jira_base=jira_base,
        jira_user=jira_user,
        jira_token=jira_token,
        user_agent=user_agent,
        jira_key_re=jira_key_re,
        ignore_pattern=ignore_pattern,
        jira_project=jira_project,
        insecure=insecure,
    )

    execute_create_release(
        gl=gl,
        project_path=project_path,
        project_name=project_name,
        root_issues=root_issues,
        jira_client=jira_client,
        jira_base=jira_base,
        jira_token=jira_token,
        jira_user=jira_user,
        jira_project=jira_project,
        gitlab_tag=gitlab_tag,
        user_agent=user_agent,
        insecure=insecure,
    )


@create_app.command("mr")
def create_mr(
    ctx: typer.Context,
    repo_url: Optional[str] = typer.Argument(None, help="Ссылка на репозиторий в GitLab (https://host/group/proj). Не требуется в batch-режиме."),
    source_branch: Optional[str] = typer.Option(None, "--from", help="Исходная ветка (source branch). В batch-режиме берётся из targets."),
    target_branch: Optional[str] = typer.Option(None, "--to", help="Целевая ветка (target branch). В batch-режиме берётся из targets."),
    # Batch-режим
    batch: bool = typer.Option(False, "--batch", help="Batch-режим: перебрать все проекты из config.yaml"),
    target: Optional[str] = typer.Option(None, "--target", help="Имя target из config.yaml (например stage, prod). Используется в batch-режиме для выбора пар веток."),
    dry_run: bool = typer.Option(
        False,
        "--dry-run",
        help="Показать, какие MR будут созданы, и какие Jira-задачи попадут в них, без фактического создания.",
    ),
    # GitLab auth
    gitlab_token: Optional[str] = typer.Option(
        default=None,
        help="Токен GitLab (env: GITLAB_TOKEN)",
        rich_help_panel="GitLab",
    ),
    gitlab_url_override: Optional[str] = typer.Option(
        default=None,
        help="База GitLab API/Host (по умолчанию берётся из repo_url).",
        rich_help_panel="GitLab",
    ),
    # Jira auth
    jira_base: Optional[str] = typer.Option(
        default=None,
        help="База Jira (из config.yaml или env JIRA_BASE)",
        rich_help_panel="Jira",
    ),
    jira_user: Optional[str] = typer.Option(
        default=None, help="Пользователь Jira (если используется basic_auth)", rich_help_panel="Jira"
    ),
    jira_token: Optional[str] = typer.Option(
        default=None, help="API-токен Jira (env: JIRA_TOKEN)", rich_help_panel="Jira"
    ),
    user_agent: Optional[str] = typer.Option(
        default=None,
        help="User-Agent для запросов к Jira.",
        rich_help_panel="Jira",
    ),
    # Поведение
    with_release: bool = typer.Option(False, "--with-release", help="Создать релиз в Jira для этого MR"),
    gitlab_tag: Optional[str] = typer.Option(None, help="Тэг для релиза (если --with-release)"),
    jira_project: Optional[str] = typer.Option(
        default=None,
        help="Фильтр по проекту Jira (например MMBT). Обязателен при --with-release.",
        rich_help_panel="Jira",
    ),
    jira_key_re: Optional[str] = typer.Option(
        default=None,
        help="Regexp для Jira-ключей",
    ),
    ignore_pattern: Optional[List[str]] = typer.Option(
        default=None,
        help="Regexp-паттерны для игнорирования коммитов по первой строке",
    ),
    insecure: Optional[bool] = typer.Option(
        default=None,
        help="Игнорировать проверку SSL-сертификатов.",
    ),
    verbose: bool = typer.Option(False, "-v", "--verbose", help="Подробный вывод"),
) -> None:
    """Создать Merge Request в GitLab (если он ещё не существует) и вывести список Jira-issue.

    Если открытый MR между ветками уже существует, используется он.

    [bold]Batch-режим[/bold]: с флагом --batch перебираются все проекты из config.yaml.
    Используйте --target для выбора пар веток (например --target stage).
    """
    setup_logging(verbose)

    cfg = ctx.obj["config"]
    d = _resolve_defaults(cfg)

    gitlab_token = gitlab_token or d["gitlab_token"]
    jira_base = jira_base or d["jira_base"]
    jira_user = jira_user or d["jira_user"] or None
    jira_token = jira_token or d["jira_token"] or None
    user_agent = user_agent or d["user_agent"]
    jira_key_re = jira_key_re or d["jira_key_re"]
    ignore_pattern = ignore_pattern if ignore_pattern is not None else d["ignore_patterns"]
    jira_project = jira_project or d["jira_project"] or None
    insecure = insecure if insecure is not None else d["gitlab_insecure"]

    if insecure:
        try:
            import urllib3  # type: ignore
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        except Exception:
            pass

    if batch:
        # Batch-режим: перебираем проекты из config.yaml
        if not target:
            raise typer.BadParameter("В batch-режиме необходимо указать --target (например --target stage)")

        projects = cfg_projects(cfg)
        if not projects:
            err_console.print("В config.yaml не определены проекты (projects).")
            raise typer.Exit(code=1)

        # Сортируем по deploy.order (если указан)
        projects_sorted = sorted(projects, key=lambda p: p.get("deploy", {}).get("order", 999))

        mr_results: List[MrResult] = []

        for proj in projects_sorted:
            proj_id = proj.get("id", proj.get("name", "unknown"))
            targets = proj.get("targets", {})
            if target not in targets:
                console.print(f"[dim]⏭  {proj_id}: target '{target}' не определён, пропускаем.[/dim]")
                continue

            proj_repo_url = proj.get("repo_url", "")
            if not proj_repo_url:
                err_console.print(f"Проект {proj_id}: не указан repo_url, пропускаем.")
                continue

            t = targets[target]
            proj_from = t.get("from", "")
            proj_to = t.get("to", "")
            if not proj_from or not proj_to:
                err_console.print(f"Проект {proj_id}: target '{target}' не содержит from/to, пропускаем.")
                continue

            console.print(f"\n[bold]{'=' * 60}[/bold]")
            console.print(f"[bold]📦 Проект: {proj_id}[/bold]  ({proj_from} → {proj_to})")
            console.print(f"[bold]{'=' * 60}[/bold]")

            try:
                result = _execute_create_mr(
                    repo_url=proj_repo_url,
                    source_branch=proj_from,
                    target_branch=proj_to,
                    gitlab_token=gitlab_token,
                    gitlab_url_override=gitlab_url_override,
                    jira_base=jira_base,
                    jira_user=jira_user,
                    jira_token=jira_token,
                    user_agent=user_agent,
                    jira_key_re=jira_key_re,
                    ignore_pattern=ignore_pattern,
                    jira_project=jira_project,
                    insecure=insecure,
                    with_release=with_release,
                    gitlab_tag=gitlab_tag,
                    dry_run=dry_run,
                    target_name=target,
                )
                if result:
                    mr_results.append(result)
            except typer.Exit:
                err_console.print(f"Ошибка при обработке проекта {proj_id}, продолжаем...")
                continue

        # Сводка по всем MR
        console.print(f"\n[bold]{'=' * 60}[/bold]")
        console.print(f"[bold green]Batch-режим завершён. Обработано проектов: {len(projects_sorted)}[/bold green]")

        if mr_results:
            console.print(f"\n[bold]Сводка MR ({len(mr_results)}):[/bold]")
            for r in mr_results:
                if dry_run:
                    status = "✔ уже существует" if not r.created else "◌ будет создан"
                else:
                    status = "✔ существующий" if not r.created else "✚ создан"
                console.print(f"  {status} | {r.project_id}: {r.title}")
                console.print(f"           {r.mr_url}")
        else:
            if dry_run:
                console.print("[yellow]В dry-run не найдено MR-кандидатов для создания.[/yellow]")
            else:
                console.print("[yellow]MR не были созданы или найдены.[/yellow]")

        return

    # Одиночный режим — repo_url обязателен
    if not repo_url:
        raise typer.BadParameter("Укажите ссылку на репозиторий (repo_url) или используйте --batch режим.")
    if not source_branch or not target_branch:
        raise typer.BadParameter("Укажите --from и --to ветки или используйте --batch --target режим.")

    _execute_create_mr(
        repo_url=repo_url,
        source_branch=source_branch,
        target_branch=target_branch,
        gitlab_token=gitlab_token,
        gitlab_url_override=gitlab_url_override,
        jira_base=jira_base,
        jira_user=jira_user,
        jira_token=jira_token,
        user_agent=user_agent,
        jira_key_re=jira_key_re,
        ignore_pattern=ignore_pattern,
        jira_project=jira_project,
        insecure=insecure,
        with_release=with_release,
        gitlab_tag=gitlab_tag,
        dry_run=dry_run,
    )


def _is_release_mr(target_name: Optional[str], target_branch: str) -> bool:
    """Определяет, является ли MR релизным.

    Релизный MR — если --target "prod" или целевая ветка "master"/"main".
    """
    if target_name and target_name.lower() == "prod":
        return True
    if target_branch.lower() in ("master", "main"):
        return True
    return False


@dataclass
class MrResult:
    """Результат обработки одного проекта в create mr."""
    project_id: str
    mr_url: str
    title: str
    created: bool  # True — создан новый, False — найден существующий


def _execute_create_mr(
    repo_url: str,
    source_branch: str,
    target_branch: str,
    gitlab_token: str,
    gitlab_url_override: Optional[str],
    jira_base: str,
    jira_user: Optional[str],
    jira_token: Optional[str],
    user_agent: str,
    jira_key_re: str,
    ignore_pattern: List[str],
    jira_project: Optional[str],
    insecure: bool,
    with_release: bool,
    gitlab_tag: Optional[str],
    dry_run: bool = False,
    target_name: Optional[str] = None,
) -> Optional[MrResult]:
    """Внутренняя логика создания MR (используется как в одиночном, так и в batch-режиме)."""
    host, project_path = parse_repo_url(repo_url)
    if not gitlab_token:
        raise typer.BadParameter("Не задан токен GitLab. Укажите --gitlab-token или env GITLAB_TOKEN")

    gitlab_base = gitlab_url_override or f"https://{host}"

    # GitLab client
    try:
        gl = build_gitlab_client(gitlab_base, gitlab_token, insecure=insecure)
    except Exception as e:
        err_console.print(f"Не удалось аутентифицироваться в GitLab: {e}")
        raise typer.Exit(code=2)

    try:
        project = gl.projects.get(project_path)
    except Exception as e:
        err_console.print(f"Ошибка при получении проекта GitLab {project_path}: {e}")
        raise typer.Exit(code=2)

    # Проверяем наличие существенных изменений (Jira-issues) перед созданием MR
    logging.info("Проверяем наличие Jira-задач в диффе %s...%s", target_branch, source_branch)
    try:
        comparison = project.repository_compare(target_branch, source_branch)
        compare_commits = [
            {
                "title": c.get("title", "") or "",
                "message": c.get("message", "") or "",
            }
            for c in comparison.get("commits", [])
        ]
        compare_diffs = comparison.get("diffs", []) or []
    except Exception as e:
        err_console.print(f"Ошибка при сравнении веток {target_branch} и {source_branch}: {e}")
        raise typer.Exit(code=3)

    # Важная проверка: бывают случаи, когда коммиты “есть”, но итоговый diff пустой.
    if not compare_diffs:
        console.print(
            f"[yellow]Нет изменений файлов (diff пуст) между '{target_branch}' и '{source_branch}'. "
            f"MR не будет создан.[/yellow]"
        )
        return None

    key_rx, ignore_rx = compile_regexps(jira_key_re, ignore_pattern)
    found_keys = extract_jira_keys_from_commits(
        commits=compare_commits,
        key_rx=key_rx,
        ignore_rx=ignore_rx,
        jira_project=jira_project,
    )

    if not found_keys:
        msg = f"No Jira issues found in diff between '{target_branch}' and '{source_branch}'"
        if jira_project:
            msg += f" for project '{jira_project}'"
        console.print(f"[yellow]{msg}. MR не будет создан.[/yellow]")
        return None

    logging.info("Найдено Jira-задач в диффе: %d. Продолжаем работу с MR.", len(found_keys))

    # Определяем, является ли MR релизным
    is_release = _is_release_mr(target_name, target_branch)
    actual_source_branch = source_branch
    mr_title = f"Merge {source_branch} into {target_branch}"
    next_tag: Optional[str] = None

    if is_release:
        logging.info("Обнаружен релизный MR (target_name=%s, target_branch=%s)", target_name, target_branch)
        try:
            tags = get_project_tags(gl, project_path)
        except Exception as e:
            err_console.print(f"Ошибка при получении тэгов GitLab: {e}")
            raise typer.Exit(code=5)

        semver_tags = [t for t in tags if parse_semver(t) is not None]

        if semver_tags:
            next_tag = compute_next_tag(tags)
            release_branch = f"release/{next_tag}"
            mr_title = f"Release {next_tag}"
            logging.info("Вычислен следующий тэг: %s, релизная ветка: %s", next_tag, release_branch)

            if dry_run:
                try:
                    project.branches.get(release_branch)
                    logging.info("DRY-RUN: релизная ветка %s уже существует.", release_branch)
                except Exception:
                    logging.info("DRY-RUN: релизная ветка %s будет создана от %s.", release_branch, source_branch)
                actual_source_branch = release_branch
            else:
                try:
                    project.branches.create({
                        "branch": release_branch,
                        "ref": source_branch,
                    })
                    logging.info("Создана релизная ветка: %s от %s", release_branch, source_branch)
                    actual_source_branch = release_branch
                except Exception as e:
                    err_msg = str(e)
                    if "already exists" in err_msg or "Branch already exists" in err_msg:
                        logging.info("Релизная ветка %s уже существует, используем её.", release_branch)
                        actual_source_branch = release_branch
                    else:
                        err_console.print(f"Ошибка при создании релизной ветки {release_branch}: {e}")
                        raise typer.Exit(code=7)
        else:
            logging.info(
                "Semver-тэги не найдены в репозитории, используем ветку %s без создания релизной ветки.",
                source_branch,
            )
            mr_title = "Release"

    # Ищем существующий открытый MR с такими же ветками
    mrs = project.mergerequests.list(
        state="opened",
        source_branch=actual_source_branch,
        target_branch=target_branch,
    )

    project_name = project_path.rsplit("/", 1)[-1]

    jira_client_for_dry_run: Optional[JIRA] = None
    root_cache: Dict[str, JiraRootIssue] = {}

    def _extract_root_issues_for_dry_run(commits: List[Dict[str, str]]) -> List[JiraRootIssue]:
        nonlocal jira_client_for_dry_run
        if jira_client_for_dry_run is None:
            try:
                jira_client_for_dry_run = build_jira_client(
                    jira_base,
                    token=jira_token,
                    user=jira_user,
                    insecure=insecure,
                    user_agent=user_agent,
                )
            except Exception as e:
                err_console.print(f"Не удалось подключиться к Jira: {e}")
                raise typer.Exit(code=4)

        return extract_root_issues_from_commits_cached(
            commits=commits,
            key_rx=key_rx,
            ignore_rx=ignore_rx,
            jira_client=jira_client_for_dry_run,
            jira_project=jira_project,
            cache=root_cache,
        )

    if mrs:
        mr = mrs[0]

        # Доп. защита: MR может существовать, но быть “пустым” по фактическим изменениям.
        try:
            mr_changes = mr.changes()
            changes_list = mr_changes.get("changes", []) if isinstance(mr_changes, dict) else []
            if not changes_list:
                console.print(
                    f"[yellow]⚠ MR существует, но diff пуст (нет изменённых файлов):[/yellow] {mr.web_url}"
                )
        except Exception as e:
            logging.debug("Не удалось получить changes для MR %s: %s", getattr(mr, "web_url", "(unknown)"), e)

        if dry_run:
            console.print(f"[cyan]DRY-RUN:[/cyan] MR уже существует и не будет создан повторно: {mr.web_url}")

            mr_commits = compare_commits
            try:
                mr_commits = [
                    {
                        "title": getattr(c, "title", "") or "",
                        "message": getattr(c, "message", "") or "",
                    }
                    for c in mr.commits()
                ]
            except Exception as e:
                logging.debug("DRY-RUN: не удалось получить commits существующего MR %s: %s", mr.web_url, e)

            root_issues = _extract_root_issues_for_dry_run(mr_commits)
            project_title = f"{project_name} (dry-run, существующий MR)"
            if root_issues:
                render_output(
                    root_issues,
                    jira_base=jira_base,
                    fmt=OutputFormat.MD,
                    mr_url=mr.web_url,
                    project_name=project_title,
                )
            else:
                print(project_title)
                print(f"MR - {mr.web_url}")
                print()
                print("- (Jira задачи не найдены в коммитах этого MR)")

            if with_release:
                if not jira_project:
                    err_console.print("Ошибка: параметр --jira-project обязателен при использовании --with-release")
                    raise typer.Exit(code=1)
                console.print("[cyan]DRY-RUN:[/cyan] --with-release указан, релиз в Jira не создаётся.")
        else:
            console.print(f"[green]✔  MR уже существует:[/green] {mr.web_url}")
        return MrResult(
            project_id=project_name,
            mr_url=mr.web_url,
            title=getattr(mr, "title", mr_title),
            created=False,
        )

    if dry_run:
        console.print(
            f"[cyan]DRY-RUN:[/cyan] будет создан MR '{mr_title}': {actual_source_branch} → {target_branch}"
        )
        root_issues = _extract_root_issues_for_dry_run(compare_commits)
        dry_run_ref = f"[dry-run] {actual_source_branch} -> {target_branch}"
        project_title = f"{project_name} (dry-run)"

        if root_issues:
            render_output(
                root_issues,
                jira_base=jira_base,
                fmt=OutputFormat.MD,
                mr_url=dry_run_ref,
                project_name=project_title,
            )
        else:
            print(project_title)
            print(f"MR - {dry_run_ref}")
            print()
            print("- (Jira задачи не найдены в коммитах кандидата MR)")

        if with_release:
            if not jira_project:
                err_console.print("Ошибка: параметр --jira-project обязателен при использовании --with-release")
                raise typer.Exit(code=1)
            console.print("[cyan]DRY-RUN:[/cyan] --with-release указан, релиз в Jira не создаётся.")

        return MrResult(
            project_id=project_name,
            mr_url=dry_run_ref,
            title=mr_title,
            created=True,
        )

    # Создаём новый MR
    logging.info("Создаём новый MR: %s -> %s", actual_source_branch, target_branch)
    try:
        mr = project.mergerequests.create({
            "source_branch": actual_source_branch,
            "target_branch": target_branch,
            "title": mr_title,
        })
        logging.info("MR создан: %s", mr.web_url)
    except Exception as e:
        err_console.print(f"Ошибка при создании MR: {e}")
        raise typer.Exit(code=7)

    # Теперь извлекаем issues
    root_issues = extract_issues_from_mr_id(
        gl=gl,
        project_path=project_path,
        iid=str(mr.iid),
        jira_base=jira_base,
        jira_user=jira_user,
        jira_token=jira_token,
        user_agent=user_agent,
        jira_key_re=jira_key_re,
        ignore_pattern=ignore_pattern,
        jira_project=jira_project,
        insecure=insecure,
        mr_url_for_log=mr.web_url,
    )

    if with_release:
        if not jira_project:
            err_console.print("Ошибка: параметр --jira-project обязателен при использовании --with-release")
            raise typer.Exit(code=1)

        # Нам нужен jira_client
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

        execute_create_release(
            gl=gl,
            project_path=project_path,
            project_name=project_name,
            root_issues=root_issues,
            jira_client=jira_client,
            jira_base=jira_base,
            jira_token=jira_token,
            jira_user=jira_user,
            jira_project=jira_project,
            gitlab_tag=gitlab_tag,
            user_agent=user_agent,
            insecure=insecure,
        )
        print()  # Пустая строка перед списком issue

    render_output(root_issues, jira_base=jira_base, fmt=OutputFormat.MD, mr_url=mr.web_url, project_name=project_name)

    return MrResult(
        project_id=project_name,
        mr_url=mr.web_url,
        title=mr_title,
        created=True,
    )


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
