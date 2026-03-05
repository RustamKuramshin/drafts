import importlib.util
import datetime as _dt
import io
import pathlib
import sys
import unittest
from contextlib import redirect_stdout


_REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
_RELMAN_PATH = _REPO_ROOT / "shell" / "relman.py"

_spec = importlib.util.spec_from_file_location("relman", str(_RELMAN_PATH))
if _spec is None or _spec.loader is None:  # pragma: no cover
    raise RuntimeError(f"Failed to load relman module from: {_RELMAN_PATH}")
relman = importlib.util.module_from_spec(_spec)
sys.modules["relman"] = relman
_spec.loader.exec_module(relman)


class RelmanPureFunctionsTest(unittest.TestCase):
    def test_parse_mr_url_ok(self) -> None:
        host, project_path, iid = relman.parse_mr_url(
            "https://gitlab.example.com/a/b/-/merge_requests/42"
        )
        self.assertEqual(host, "gitlab.example.com")
        self.assertEqual(project_path, "a/b")
        self.assertEqual(iid, "42")

    def test_parse_repo_url_ok(self) -> None:
        host, project_path = relman.parse_repo_url("https://gitlab.example.com/a/b")
        self.assertEqual(host, "gitlab.example.com")
        self.assertEqual(project_path, "a/b")

    def test_compute_next_tag(self) -> None:
        self.assertEqual(relman.compute_next_tag(["0.1.0", "0.2.3", "v0.10.0"]), "0.10.1")

    def test_compile_regexps_and_extract_keys(self) -> None:
        key_rx, ignore_rx = relman.compile_regexps(r"MMBT-\d+", [r"^Merge branch"])
        commits = [
            {"title": "MMBT-1 a", "message": ""},
            {"title": "Merge branch 'x'", "message": "MMBT-2 b"},
            {"title": "", "message": "MMBT-3 c"},
        ]
        keys = relman.extract_jira_keys_from_commits(commits, key_rx=key_rx, ignore_rx=ignore_rx)
        self.assertEqual(keys, {"MMBT-1", "MMBT-3"})

    def test_build_fix_skip_ci_branch_name_uses_minutes_without_seconds(self) -> None:
        now = _dt.datetime(2026, 2, 26, 20, 24, 59)
        self.assertEqual(relman.build_fix_skip_ci_branch_name(now), "chore/fix-skip-ci-20260226-2024")

    def test_append_empty_line_adds_blank_line_at_end(self) -> None:
        self.assertEqual(relman._append_empty_line("a\n"), "a\n\n")
        self.assertEqual(relman._append_empty_line("a"), "a\n\n")
        # Даже если файл уже оканчивается пустой строкой, мы должны гарантировать изменение,
        # иначе GitLab может создать «пустой» коммит/ MR без diff.
        self.assertEqual(relman._append_empty_line("a\n\n"), "a\n\n\n")

    def test_decode_gitlab_file_content_handles_bytes_without_repr(self) -> None:
        class _F:
            def decode(self) -> bytes:  # type: ignore[override]
                return b"line1\nline2\n"

        txt = relman._decode_gitlab_file_content(_F())
        self.assertEqual(txt, "line1\nline2\n")
        self.assertNotIn("b'", txt)

    def test_render_output_tree_shows_actual_issues_under_root(self) -> None:
        roots = [
            relman.JiraRootIssue("MMBT-1", "Root summary", "Epic"),
        ]
        children_by_root = {
            "MMBT-1": [
                relman.JiraRootIssue("MMBT-2", "Child summary", "Task"),
            ]
        }

        buf = io.StringIO()
        with redirect_stdout(buf):
            relman.render_output(
                roots,
                jira_base="https://jira.example.com",
                fmt=relman.OutputFormat.MD,
                mr_url="MR",
                project_name="proj",
                children_by_root=children_by_root,
            )

        out = buf.getvalue()
        self.assertIn("|__ MMBT-2:", out)
        self.assertIn("https://jira.example.com/browse/MMBT-2", out)
        self.assertIn("\n      https://jira.example.com/browse/MMBT-2\n", out)
        self.assertNotIn("\n  |   https://jira.example.com/browse/MMBT-2\n", out)


if __name__ == "__main__":
    unittest.main(verbosity=2)
