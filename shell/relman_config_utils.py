from __future__ import annotations

import urllib.parse
from typing import Any, Dict, List, Optional


def _is_http_url(value: str) -> bool:
    p = urllib.parse.urlparse(value)
    return p.scheme in ("http", "https") and bool(p.netloc)


def _projects(cfg: Dict[str, Any]) -> List[Dict[str, Any]]:
    projects = cfg.get("projects", [])
    if isinstance(projects, list):
        return [p for p in projects if isinstance(p, dict)]
    return []


def _unique_project_match(
    *, cfg: Dict[str, Any], selector: str, allow_match_by_repo_url: bool
) -> Dict[str, Any]:
    projs = _projects(cfg)

    matches: List[Dict[str, Any]] = [p for p in projs if p.get("name") == selector]
    if not matches:
        matches = [p for p in projs if p.get("id") == selector]
    if not matches and allow_match_by_repo_url:
        matches = [p for p in projs if p.get("repo_url") == selector]

    if not matches:
        known = sorted({p.get("name") for p in projs if p.get("name")})
        if known:
            raise ValueError(
                f"Проект '{selector}' не найден в config.yaml. Доступные name: {', '.join(known)}"
            )
        raise ValueError(f"Проект '{selector}' не найден в config.yaml (projects пуст).")

    if len(matches) > 1:
        ids = [str(p.get("id") or p.get("name") or "unknown") for p in matches]
        raise ValueError(
            f"Найдено несколько проектов для '{selector}' в config.yaml: {', '.join(ids)}"
        )

    return matches[0]


def resolve_repo_url_from_config(cfg: Dict[str, Any], repo_or_name: str) -> str:
    """Вернуть repo_url.

    Правила:
    - Если `repo_or_name` похож на http(s)-URL — возвращаем как есть.
    - Иначе ищем проект в config.yaml по `projects[].name` (затем по `id`) и берём `repo_url`.
    """
    if _is_http_url(repo_or_name):
        return repo_or_name

    proj = _unique_project_match(cfg=cfg, selector=repo_or_name, allow_match_by_repo_url=False)
    repo_url = proj.get("repo_url")
    if not repo_url or not isinstance(repo_url, str):
        proj_id = proj.get("id") or proj.get("name") or "unknown"
        raise ValueError(f"Проект {proj_id}: в config.yaml не указан repo_url")
    return repo_url


def select_projects_for_batch(cfg: Dict[str, Any], selector: Optional[str]) -> List[Dict[str, Any]]:
    """Вернуть список проектов для batch-обработки.

    Если selector не задан — вернём все проекты.
    Если selector задан — он может быть:
    - name/id проекта из config.yaml
    - либо полный repo_url, который есть в config.yaml
    """
    if selector is None:
        return _projects(cfg)

    allow_repo_url = _is_http_url(selector)
    proj = _unique_project_match(cfg=cfg, selector=selector, allow_match_by_repo_url=allow_repo_url)
    return [proj]
