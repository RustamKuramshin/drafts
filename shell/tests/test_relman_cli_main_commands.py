import json
import datetime as _dt
import pathlib
import re
import sys
import importlib.util
import tempfile
import unittest
from unittest.mock import patch

import yaml
from typer.testing import CliRunner


_REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
_RELMAN_PATH = _REPO_ROOT / "shell" / "relman.py"

_spec = importlib.util.spec_from_file_location("relman", str(_RELMAN_PATH))
if _spec is None or _spec.loader is None:  # pragma: no cover
    raise RuntimeError(f"Failed to load relman module from: {_RELMAN_PATH}")
relman = importlib.util.module_from_spec(_spec)
sys.modules["relman"] = relman
_spec.loader.exec_module(relman)

from relman_test_fakes import (
    FakeCommit,
    FakeGitlab,
    FakeJira,
    FakeJiraIssue,
    FakeMR,
    FakeProject,
    FakeResponse,
    strip_ansi,
)


class RelmanCliMainCommandsTest(unittest.TestCase):
    def setUp(self) -> None:
        # В relman.py есть кэш конфига на уровне модуля.
        relman._loaded_config = {}
        self.runner = CliRunner()

    def _write_cfg(self, cfg: dict) -> pathlib.Path:
        td = tempfile.TemporaryDirectory()
        self.addCleanup(td.cleanup)
        cfg_path = pathlib.Path(td.name) / "config.yaml"
        cfg_path.write_text(yaml.safe_dump(cfg, allow_unicode=True, sort_keys=False), encoding="utf-8")
        return cfg_path

    def test_get_issues_supports_json_and_urls_output_without_network(self) -> None:
        cfg_path = self._write_cfg({"version": 1, "defaults": {}, "projects": []})
        mr_url = "https://gitlab.example.com/group/proj/-/merge_requests/123"

        mr = FakeMR(
            iid=123,
            title="Test MR",
            web_url=mr_url,
            source_branch="dev",
            target_branch="stage",
            commits=[
                FakeCommit(title="MMBT-1 do thing", message=""),
                FakeCommit(title="Merge branch 'x' into 'y'", message=""),
            ],
        )
        fake_gl = FakeGitlab({"group/proj": FakeProject(mrs=[mr])})

        fake_jira = FakeJira(
            {
                "MMBT-1": FakeJiraIssue(key="MMBT-1", summary="Issue 1", issuetype="Task"),
            }
        )

        def _no_network(*args: object, **kwargs: object) -> None:  # pragma: no cover
            raise AssertionError("Unexpected network call via requests")

        with (
            patch.object(relman, "build_gitlab_client", return_value=fake_gl),
            patch.object(relman, "build_jira_client", return_value=fake_jira),
            patch.object(relman._requests, "post", side_effect=_no_network),
            patch.object(relman._requests, "put", side_effect=_no_network),
        ):
            res_json = self.runner.invoke(
                relman.app,
                [
                    "--config",
                    str(cfg_path),
                    "get",
                    "issues",
                    mr_url,
                    "--gitlab-token",
                    "gl-token",
                    "--jira-base",
                    "https://jira.example",
                    "--jira-token",
                    "jira-token",
                    "--jira-key-re",
                    r"MMBT-\d+",
                    "--ignore-pattern",
                    r"^Merge branch",
                    "--fmt",
                    "json",
                ],
            )
            self.assertEqual(res_json.exit_code, 0, msg=strip_ansi(res_json.output))
            payload = json.loads(strip_ansi(res_json.output).strip())
            self.assertEqual(payload[0]["key"], "MMBT-1")
            self.assertIn("https://jira.example/browse/MMBT-1", payload[0]["url"])

            # urls
            relman._loaded_config = {}
            res_urls = self.runner.invoke(
                relman.app,
                [
                    "--config",
                    str(cfg_path),
                    "get",
                    "issues",
                    mr_url,
                    "--gitlab-token",
                    "gl-token",
                    "--jira-base",
                    "https://jira.example",
                    "--jira-token",
                    "jira-token",
                    "--jira-key-re",
                    r"MMBT-\d+",
                    "--ignore-pattern",
                    r"^Merge branch",
                    "--fmt",
                    "urls",
                ],
            )
            self.assertEqual(res_urls.exit_code, 0, msg=strip_ansi(res_urls.output))
            self.assertIn("https://jira.example/browse/MMBT-1", strip_ansi(res_urls.output))

    def test_get_mrs_batch_runs_without_network(self) -> None:
        cfg_path = self._write_cfg(
            {
                "version": 1,
                "defaults": {},
                "projects": [
                    {
                        "id": "p1",
                        "name": "p1",
                        "repo_url": "https://gitlab.example.com/group/proj",
                        "targets": {"stage": {"from": "dev", "to": "stage"}},
                    }
                ],
            }
        )

        mr = FakeMR(
            iid=10,
            title="Dev->Stage",
            web_url="https://gitlab.example.com/group/proj/-/merge_requests/10",
            source_branch="dev",
            target_branch="stage",
            commits=[FakeCommit(title="MMBT-1 do thing", message="")],
        )
        fake_gl = FakeGitlab({"group/proj": FakeProject(mrs=[mr])})
        fake_jira = FakeJira({"MMBT-1": FakeJiraIssue(key="MMBT-1", summary="Issue 1")})

        def _no_network(*args: object, **kwargs: object) -> None:  # pragma: no cover
            raise AssertionError("Unexpected network call via requests")

        with (
            patch.object(relman, "build_gitlab_client", return_value=fake_gl),
            patch.object(relman, "build_jira_client", return_value=fake_jira),
            patch.object(relman._requests, "post", side_effect=_no_network),
            patch.object(relman._requests, "put", side_effect=_no_network),
        ):
            res = self.runner.invoke(
                relman.app,
                [
                    "--config",
                    str(cfg_path),
                    "get",
                    "mrs",
                    "--batch",
                    "--target",
                    "stage",
                    "--gitlab-token",
                    "gl-token",
                    "--jira-base",
                    "https://jira.example",
                    "--jira-token",
                    "jira-token",
                    "--jira-key-re",
                    r"MMBT-\d+",
                    "--fmt",
                    "urls",
                ],
            )
            self.assertEqual(res.exit_code, 0, msg=strip_ansi(res.output))
            out = strip_ansi(res.output)
            self.assertIn("https://gitlab.example.com/group/proj/-/merge_requests/10", out)
            self.assertIn("https://jira.example/browse/MMBT-1", out)

    def test_get_mrs_batch_skip_ci_filters_only_matching_mrs(self) -> None:
        cfg_path = self._write_cfg(
            {
                "version": 1,
                "defaults": {},
                "projects": [
                    {
                        "id": "p1",
                        "name": "p1",
                        "repo_url": "https://gitlab.example.com/group/proj",
                        "targets": {"stage": {"from": "dev", "to": "stage"}},
                    }
                ],
            }
        )

        mr_skip = FakeMR(
            iid=10,
            title="Dev->Stage",
            web_url="https://gitlab.example.com/group/proj/-/merge_requests/10",
            source_branch="dev",
            target_branch="stage",
            commits=[
                FakeCommit(title="MMBT-1 old commit", message="", committed_date="2026-01-01T10:00:00+00:00"),
                FakeCommit(title="[SKIP CI]", message="MMBT-1 do thing", committed_date="2026-01-02T10:00:00+00:00"),
            ],
        )
        mr_no_skip = FakeMR(
            iid=11,
            title="Dev->Stage",
            web_url="https://gitlab.example.com/group/proj/-/merge_requests/11",
            source_branch="dev",
            target_branch="stage",
            commits=[
                FakeCommit(title="[skip ci]", message="old", committed_date="2026-01-01T10:00:00+00:00"),
                FakeCommit(title="MMBT-2 normal", message="", committed_date="2026-01-02T10:00:00+00:00"),
            ],
        )

        fake_gl = FakeGitlab({"group/proj": FakeProject(mrs=[mr_skip, mr_no_skip])})
        fake_jira = FakeJira(
            {
                "MMBT-1": FakeJiraIssue(key="MMBT-1", summary="Issue 1"),
                "MMBT-2": FakeJiraIssue(key="MMBT-2", summary="Issue 2"),
            }
        )

        def _no_network(*args: object, **kwargs: object) -> None:  # pragma: no cover
            raise AssertionError("Unexpected network call via requests")

        with (
            patch.object(relman, "build_gitlab_client", return_value=fake_gl),
            patch.object(relman, "build_jira_client", return_value=fake_jira),
            patch.object(relman._requests, "post", side_effect=_no_network),
            patch.object(relman._requests, "put", side_effect=_no_network),
        ):
            res = self.runner.invoke(
                relman.app,
                [
                    "--config",
                    str(cfg_path),
                    "get",
                    "mrs",
                    "--batch",
                    "--target",
                    "stage",
                    "--skip-ci",
                    "--gitlab-token",
                    "gl-token",
                    "--jira-base",
                    "https://jira.example",
                    "--jira-token",
                    "jira-token",
                    "--jira-key-re",
                    r"MMBT-\d+",
                    "--fmt",
                    "urls",
                ],
            )
            self.assertEqual(res.exit_code, 0, msg=strip_ansi(res.output))
            out = strip_ansi(res.output)
            self.assertIn("https://gitlab.example.com/group/proj/-/merge_requests/10", out)
            self.assertIn("https://jira.example/browse/MMBT-1", out)
            self.assertNotIn("https://gitlab.example.com/group/proj/-/merge_requests/11", out)
            self.assertNotIn("https://jira.example/browse/MMBT-2", out)

    def test_create_release_requires_jira_project(self) -> None:
        cfg_path = self._write_cfg({"version": 1, "defaults": {}, "projects": []})
        mr_url = "https://gitlab.example.com/group/proj/-/merge_requests/123"

        with (
            patch.object(relman, "extract_issues_from_mr") as extract_mock,
            patch.object(relman, "execute_create_release") as exec_mock,
        ):
            res = self.runner.invoke(
                relman.app,
                [
                    "--config",
                    str(cfg_path),
                    "create",
                    "release",
                    mr_url,
                    "--gitlab-token",
                    "gl-token",
                    "--jira-base",
                    "https://jira.example",
                    "--jira-token",
                    "jira-token",
                ],
            )

        self.assertNotEqual(res.exit_code, 0)
        self.assertIn("Не задан проект Jira", strip_ansi(res.output))
        extract_mock.assert_not_called()
        exec_mock.assert_not_called()

    def test_create_release_success_uses_requests_post_put_but_no_real_network(self) -> None:
        cfg_path = self._write_cfg({"version": 1, "defaults": {}, "projects": []})
        mr_url = "https://gitlab.example.com/group/proj/-/merge_requests/123"

        mr = FakeMR(
            iid=123,
            title="Test MR",
            web_url=mr_url,
            source_branch="dev",
            target_branch="stage",
            commits=[
                FakeCommit(title="MMBT-1 do thing", message=""),
                FakeCommit(title="MMBT-2 do other", message=""),
            ],
        )
        fake_gl = FakeGitlab({"group/proj": FakeProject(mrs=[mr])})
        fake_jira = FakeJira(
            {
                "MMBT-1": FakeJiraIssue(key="MMBT-1", summary="Issue 1", fix_versions=[]),
                "MMBT-2": FakeJiraIssue(key="MMBT-2", summary="Issue 2", fix_versions=[]),
            }
        )

        post_calls: list[tuple[tuple[object, ...], dict]] = []
        put_calls: list[tuple[tuple[object, ...], dict]] = []

        def _fake_post(*args: object, **kwargs: object) -> FakeResponse:
            post_calls.append((args, dict(kwargs)))
            return FakeResponse()

        def _fake_put(*args: object, **kwargs: object) -> FakeResponse:
            put_calls.append((args, dict(kwargs)))
            return FakeResponse()

        with (
            patch.object(relman, "build_gitlab_client", return_value=fake_gl),
            patch.object(relman, "build_jira_client", return_value=fake_jira),
            patch.object(relman._requests, "post", side_effect=_fake_post),
            patch.object(relman._requests, "put", side_effect=_fake_put),
        ):
            res = self.runner.invoke(
                relman.app,
                [
                    "--config",
                    str(cfg_path),
                    "create",
                    "release",
                    mr_url,
                    "--gitlab-token",
                    "gl-token",
                    "--jira-base",
                    "https://jira.example",
                    "--jira-token",
                    "jira-token",
                    "--jira-project",
                    "MMBT",
                    "--jira-key-re",
                    r"MMBT-\d+",
                    "--gitlab-tag",
                    "1.2.3",
                ],
            )

        self.assertEqual(res.exit_code, 0, msg=strip_ansi(res.output))
        self.assertGreaterEqual(len(post_calls), 1)
        self.assertEqual(len(put_calls), 2)
        out = strip_ansi(res.output)
        self.assertIn("Создан релиз", out)

    def test_create_mr_dry_run_runs_without_network(self) -> None:
        cfg_path = self._write_cfg({"version": 1, "defaults": {}, "projects": []})

        compare_payload = {
            "commits": [{"title": "MMBT-1 do thing", "message": ""}],
            "diffs": [{"old_path": "a", "new_path": "a"}],
        }
        fake_gl = FakeGitlab(
            {
                "group/proj": FakeProject(
                    mrs=[],
                    compare_payload=compare_payload,
                )
            }
        )
        fake_jira = FakeJira({"MMBT-1": FakeJiraIssue(key="MMBT-1", summary="Issue 1")})

        def _no_network(*args: object, **kwargs: object) -> None:  # pragma: no cover
            raise AssertionError("Unexpected network call via requests")

        with (
            patch.object(relman, "build_gitlab_client", return_value=fake_gl),
            patch.object(relman, "build_jira_client", return_value=fake_jira),
            patch.object(relman._requests, "post", side_effect=_no_network),
            patch.object(relman._requests, "put", side_effect=_no_network),
        ):
            res = self.runner.invoke(
                relman.app,
                [
                    "--config",
                    str(cfg_path),
                    "create",
                    "mr",
                    "https://gitlab.example.com/group/proj",
                    "--from",
                    "dev",
                    "--to",
                    "stage",
                    "--dry-run",
                    "--gitlab-token",
                    "gl-token",
                    "--jira-base",
                    "https://jira.example",
                    "--jira-token",
                    "jira-token",
                    "--jira-key-re",
                    r"MMBT-\d+",
                ],
            )

        self.assertEqual(res.exit_code, 0, msg=strip_ansi(res.output))
        out = strip_ansi(res.output)
        self.assertIn("DRY-RUN", out)
        self.assertIn("MMBT-1", out)

    def test_create_mr_batch_dry_run_calls_execute_per_project(self) -> None:
        cfg_path = self._write_cfg(
            {
                "version": 1,
                "defaults": {},
                "projects": [
                    {
                        "id": "p1",
                        "name": "p1",
                        "repo_url": "https://gitlab.example.com/group/proj1",
                        "targets": {"stage": {"from": "dev", "to": "stage"}},
                    },
                    {
                        "id": "p2",
                        "name": "p2",
                        "repo_url": "https://gitlab.example.com/group/proj2",
                        "targets": {"stage": {"from": "dev", "to": "stage"}},
                    },
                ],
            }
        )

        def _no_network(*args: object, **kwargs: object) -> None:  # pragma: no cover
            raise AssertionError("Unexpected network call")

        with (
            patch.object(relman, "_execute_create_mr", side_effect=[
                relman.MrResult(project_id="p1", mr_url="[dry-run] x", title="t", created=True),
                None,
            ]) as exec_mock,
            patch.object(relman._requests, "post", side_effect=_no_network),
            patch.object(relman._requests, "put", side_effect=_no_network),
        ):
            res = self.runner.invoke(
                relman.app,
                [
                    "--config",
                    str(cfg_path),
                    "create",
                    "mr",
                    "--batch",
                    "--target",
                    "stage",
                    "--dry-run",
                    "--gitlab-token",
                    "gl-token",
                    "--jira-base",
                    "https://jira.example",
                    "--jira-token",
                    "jira-token",
                ],
            )

        self.assertEqual(res.exit_code, 0, msg=strip_ansi(res.output))
        self.assertEqual(exec_mock.call_count, 2)

    def test_fix_skip_ci_single_creates_technical_mr(self) -> None:
        cfg_path = self._write_cfg({"version": 1, "defaults": {}, "projects": []})
        mr_url = "https://gitlab.example.com/group/proj/-/merge_requests/123"

        mr = FakeMR(
            iid=123,
            title="Blocked",
            web_url=mr_url,
            source_branch="dev",
            target_branch="stage",
            commits=[
                FakeCommit(title="MMBT-1 normal", message="", committed_date="2026-01-01T10:00:00+00:00"),
                FakeCommit(title="[skip ci]", message="MMBT-1 do thing", committed_date="2026-01-02T10:00:00+00:00"),
            ],
        )
        fake_project = FakeProject(mrs=[mr], files={"README.md": "hello\n"})
        fake_gl = FakeGitlab({"group/proj": fake_project})

        with patch.object(relman, "build_gitlab_client", return_value=fake_gl):
            res = self.runner.invoke(
                relman.app,
                [
                    "--config",
                    str(cfg_path),
                    "fix",
                    "skip-ci",
                    mr_url,
                    "--gitlab-token",
                    "gl-token",
                ],
            )

        self.assertEqual(res.exit_code, 0, msg=strip_ansi(res.output))
        out = strip_ansi(res.output)
        self.assertIn("Созданные технические MR", out)
        self.assertIn("https://gitlab.example.com/mr/124", out)

        created_branches = [b for b in fake_project.branches._existing if b.startswith("chore/fix-skip-ci-")]
        self.assertEqual(len(created_branches), 1)
        self.assertRegex(created_branches[0], r"^chore/fix-skip-ci-\d{8}-\d{4}(-\d+)?(-\d+)?$")
        self.assertEqual(len(fake_project.commits.create_calls), 1)

        last_mr_payload = fake_project.mergerequests.last_create_payload or {}
        self.assertEqual(last_mr_payload.get("target_branch"), "dev")
        self.assertTrue(last_mr_payload.get("remove_source_branch"))

        updated_readme = fake_project.files.get(file_path="README.md", ref="dev").decode()
        self.assertTrue(updated_readme.endswith("\n\n"))

    def test_fix_skip_ci_batch_creates_only_for_matching_mrs(self) -> None:
        cfg_path = self._write_cfg(
            {
                "version": 1,
                "defaults": {},
                "projects": [
                    {
                        "id": "p1",
                        "name": "p1",
                        "repo_url": "https://gitlab.example.com/group/proj",
                        "targets": {"stage": {"from": "dev", "to": "stage"}},
                    }
                ],
            }
        )

        mr_skip = FakeMR(
            iid=10,
            title="Dev->Stage",
            web_url="https://gitlab.example.com/group/proj/-/merge_requests/10",
            source_branch="dev",
            target_branch="stage",
            commits=[
                FakeCommit(title="MMBT-1 normal", message="", committed_date="2026-01-01T10:00:00+00:00"),
                FakeCommit(title="[SKIP CI]", message="MMBT-1 do thing", committed_date="2026-01-02T10:00:00+00:00"),
            ],
        )
        mr_no_skip = FakeMR(
            iid=11,
            title="Dev->Stage",
            web_url="https://gitlab.example.com/group/proj/-/merge_requests/11",
            source_branch="dev",
            target_branch="stage",
            commits=[
                FakeCommit(title="[skip ci] old", message="", committed_date="2026-01-01T10:00:00+00:00"),
                FakeCommit(title="MMBT-2 normal", message="", committed_date="2026-01-02T10:00:00+00:00"),
            ],
        )

        fake_project = FakeProject(mrs=[mr_skip, mr_no_skip], files={"README.md": "hello\n"})
        fake_gl = FakeGitlab({"group/proj": fake_project})

        with patch.object(relman, "build_gitlab_client", return_value=fake_gl):
            res = self.runner.invoke(
                relman.app,
                [
                    "--config",
                    str(cfg_path),
                    "fix",
                    "skip-ci",
                    "--batch",
                    "--target",
                    "stage",
                    "--gitlab-token",
                    "gl-token",
                ],
            )

        self.assertEqual(res.exit_code, 0, msg=strip_ansi(res.output))
        out = strip_ansi(res.output)
        self.assertIn("Созданные технические MR", out)
        self.assertEqual(len(fake_project.mergerequests.create_calls), 1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
