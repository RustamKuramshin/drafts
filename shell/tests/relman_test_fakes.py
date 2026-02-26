import re
from dataclasses import dataclass
from types import SimpleNamespace
from typing import Any, Dict, Iterable, List, Optional


_ANSI_RE = re.compile(r"\x1b\[[0-9;]*m")


def strip_ansi(text: str) -> str:
    return _ANSI_RE.sub("", text or "")


@dataclass
class FakeCommit:
    title: str = ""
    message: str = ""
    committed_date: str = ""


class FakeMR:
    def __init__(
        self,
        *,
        iid: int,
        title: str = "",
        web_url: str = "",
        source_branch: str = "",
        target_branch: str = "",
        commits: Optional[List[FakeCommit]] = None,
        changes_payload: Optional[Dict[str, Any]] = None,
    ) -> None:
        self.iid = iid
        self.title = title
        self.web_url = web_url
        self.source_branch = source_branch
        self.target_branch = target_branch
        self._commits = commits or []
        self._changes_payload = changes_payload or {"changes": [{"old_path": "a", "new_path": "a"}]}

    def commits(self) -> List[FakeCommit]:
        return list(self._commits)

    def changes(self) -> Dict[str, Any]:
        return dict(self._changes_payload)


class FakeMergeRequests:
    def __init__(self, *, mrs: Iterable[FakeMR]) -> None:
        self._mrs_by_iid: Dict[int, FakeMR] = {int(m.iid): m for m in mrs}
        self.create_calls: List[Dict[str, Any]] = []
        self.last_create_payload: Optional[Dict[str, Any]] = None

    def list(self, **kwargs: Any) -> List[FakeMR]:
        # Минимальная реализация фильтрации, достаточная для _list_open_mrs_for_env.
        state = kwargs.get("state")
        if state and state != "opened":
            return []

        src = kwargs.get("source_branch")
        tgt = kwargs.get("target_branch")

        result: List[FakeMR] = []
        for mr in self._mrs_by_iid.values():
            if src is not None and mr.source_branch != src:
                continue
            if tgt is not None and mr.target_branch != tgt:
                continue
            result.append(mr)
        return result

    def get(self, iid: int | str) -> FakeMR:
        ii = int(iid)
        if ii not in self._mrs_by_iid:
            raise KeyError(f"MR {iid} not found")
        return self._mrs_by_iid[ii]

    def create(self, payload: Dict[str, Any]) -> FakeMR:
        # Создаём MR с новым IID.
        self.create_calls.append(dict(payload))
        self.last_create_payload = dict(payload)
        next_iid = (max(self._mrs_by_iid.keys()) + 1) if self._mrs_by_iid else 1
        mr = FakeMR(
            iid=next_iid,
            title=payload.get("title", ""),
            web_url=f"https://gitlab.example.com/mr/{next_iid}",
            source_branch=payload.get("source_branch", ""),
            target_branch=payload.get("target_branch", ""),
        )
        # Доп. атрибуты, которые могут быть важны для тестов.
        if "remove_source_branch" in payload:
            setattr(mr, "remove_source_branch", payload.get("remove_source_branch"))
        self._mrs_by_iid[next_iid] = mr
        return mr


@dataclass
class FakeFile:
    file_path: str
    content: str

    def decode(self) -> str:
        return self.content


class FakeFiles:
    def __init__(self, files: Dict[str, str]) -> None:
        # file_path -> content
        self._files = dict(files)

    def get(self, file_path: str, ref: str = "") -> FakeFile:
        _ = ref
        if file_path not in self._files:
            raise KeyError(f"file {file_path} not found")
        return FakeFile(file_path=file_path, content=self._files[file_path])

    def _update_content(self, file_path: str, content: str) -> None:
        self._files[file_path] = content


class FakeCommits:
    def __init__(self, files: FakeFiles) -> None:
        self._files = files
        self.create_calls: List[Dict[str, Any]] = []

    def create(self, payload: Dict[str, Any]) -> Any:
        # Минимально поддерживаем actions для update.
        self.create_calls.append(dict(payload))
        actions = payload.get("actions") or []
        for a in actions:
            if a.get("action") == "update":
                fp = a.get("file_path")
                if fp:
                    self._files._update_content(fp, a.get("content", ""))
        return SimpleNamespace(id="fake-commit")


class FakeTags:
    def __init__(self, names: List[str]) -> None:
        self._names = list(names)

    def list(self, all: bool = False) -> List[Any]:
        return [SimpleNamespace(name=n) for n in self._names]


class FakeBranches:
    def __init__(self, existing: Optional[List[str]] = None) -> None:
        self._existing = set(existing or [])

    def get(self, name: str) -> Any:
        if name not in self._existing:
            raise KeyError(name)
        return SimpleNamespace(name=name)

    def create(self, payload: Dict[str, Any]) -> Any:
        name = payload.get("branch")
        if not name:
            raise ValueError("branch is required")
        self._existing.add(name)
        return SimpleNamespace(name=name)


class FakeProject:
    def __init__(
        self,
        *,
        mrs: Iterable[FakeMR],
        tags: Optional[List[str]] = None,
        compare_payload: Optional[Dict[str, Any]] = None,
        branches: Optional[List[str]] = None,
        files: Optional[Dict[str, str]] = None,
    ) -> None:
        self.mergerequests = FakeMergeRequests(mrs=mrs)
        self.tags = FakeTags(tags or [])
        self.branches = FakeBranches(existing=branches)
        self.files = FakeFiles(files or {})
        self.commits = FakeCommits(self.files)
        self._compare_payload = compare_payload or {
            "commits": [{"title": "MMBT-1 commit", "message": ""}],
            "diffs": [{"old_path": "a", "new_path": "a"}],
        }

    def repository_compare(self, from_ref: str, to_ref: str) -> Dict[str, Any]:
        # В python-gitlab порядок параметров: compare(from, to). В relman вызывается
        # repository_compare(target, source) — так и оставляем.
        _ = (from_ref, to_ref)
        return dict(self._compare_payload)


class FakeProjects:
    def __init__(self, mapping: Dict[str, FakeProject]) -> None:
        self._mapping = dict(mapping)

    def get(self, project_path: str) -> FakeProject:
        if project_path not in self._mapping:
            raise KeyError(f"project {project_path} not found")
        return self._mapping[project_path]


class FakeGitlab:
    def __init__(self, projects: Dict[str, FakeProject]) -> None:
        self.projects = FakeProjects(projects)

    def auth(self) -> None:
        return


class FakeJiraIssue:
    def __init__(
        self,
        *,
        key: str,
        summary: str = "",
        issuetype: str = "Task",
        parent_key: Optional[str] = None,
        fix_versions: Optional[List[str]] = None,
    ) -> None:
        self.key = key
        parent_obj = SimpleNamespace(key=parent_key) if parent_key else None
        self.fields = SimpleNamespace(
            summary=summary,
            issuetype=SimpleNamespace(name=issuetype),
            parent=parent_obj,
            fixVersions=[SimpleNamespace(name=n) for n in (fix_versions or [])],
        )


class FakeJira:
    def __init__(self, issues: Dict[str, FakeJiraIssue]) -> None:
        self._issues = dict(issues)

    def issue(self, key: str, fields: str = "") -> FakeJiraIssue:
        if key not in self._issues:
            raise KeyError(f"issue {key} not found")
        return self._issues[key]


class FakeResponse:
    def __init__(self, *, status_code: int = 200, text: str = "") -> None:
        self.status_code = status_code
        self.text = text

    def raise_for_status(self) -> None:
        return
