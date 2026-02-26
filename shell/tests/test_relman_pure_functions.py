import importlib.util
import pathlib
import sys
import unittest


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
        self.assertEqual(relman.compute_next_tag(["0.1.0", "0.2.3", "v0.10.0"]), "0.11.0")

    def test_compile_regexps_and_extract_keys(self) -> None:
        key_rx, ignore_rx = relman.compile_regexps(r"MMBT-\d+", [r"^Merge branch"])
        commits = [
            {"title": "MMBT-1 a", "message": ""},
            {"title": "Merge branch 'x'", "message": "MMBT-2 b"},
            {"title": "", "message": "MMBT-3 c"},
        ]
        keys = relman.extract_jira_keys_from_commits(commits, key_rx=key_rx, ignore_rx=ignore_rx)
        self.assertEqual(keys, {"MMBT-1", "MMBT-3"})


if __name__ == "__main__":
    unittest.main(verbosity=2)
