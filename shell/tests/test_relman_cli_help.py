import os
import pathlib
import re
import subprocess
import sys
import tempfile
import unittest


_ANSI_RE = re.compile(r"\x1b\[[0-9;]*m")


def _strip_ansi(text: str) -> str:
    return _ANSI_RE.sub("", text)


def _debug_proc(proc: subprocess.CompletedProcess[str]) -> str:
    stdout = _strip_ansi(proc.stdout or "")
    stderr = _strip_ansi(proc.stderr or "")
    return (
        "\n".join(
            [
                f"returncode: {proc.returncode}",
                "--- stdout ---",
                stdout,
                "--- stderr ---",
                stderr,
            ]
        )
        + "\n"
    )


class RelmanCliHelpTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.repo_root = pathlib.Path(__file__).resolve().parents[2]
        cls.relman_py = cls.repo_root / "shell" / "relman.py"
        if not cls.relman_py.is_file():
            raise RuntimeError(f"relman.py not found at: {cls.relman_py}")

    def _write_min_config(self, directory: pathlib.Path) -> pathlib.Path:
        # Минимальный config.yaml, достаточный чтобы relman.py смог его загрузить
        # в callback до показа help для подкоманд.
        cfg_path = directory / "config.yaml"
        cfg_path.write_text(
            "version: 1\n\n"
            "defaults: {}\n\n"
            "projects: []\n",
            encoding="utf-8",
        )
        return cfg_path

    def _run_relman(self, args: list[str], *, config_path: pathlib.Path | None = None) -> subprocess.CompletedProcess[str]:
        cmd = [sys.executable, str(self.relman_py)]
        if config_path is not None:
            cmd += ["--config", str(config_path)]
        cmd += args

        env = os.environ.copy()
        env.setdefault("PYTHONUTF8", "1")

        return subprocess.run(
            cmd,
            cwd=str(self.repo_root),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            env=env,
        )

    def test_root_help(self) -> None:
        proc = self._run_relman(["--help"])
        self.assertEqual(proc.returncode, 0, msg=_debug_proc(proc))

        out = _strip_ansi(proc.stdout)
        self.assertIn("Usage: relman.py", out)
        self.assertIn("Commands", out)
        self.assertIn("get", out)
        self.assertIn("create", out)
        self.assertIn("Примеры", out)

    def test_get_help(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            cfg = self._write_min_config(pathlib.Path(td))
            proc = self._run_relman(["get", "--help"], config_path=cfg)

        self.assertEqual(proc.returncode, 0, msg=_debug_proc(proc))
        out = _strip_ansi(proc.stdout)
        self.assertIn("Usage: relman.py get", out)
        self.assertIn("Commands", out)
        self.assertIn("issues", out)
        self.assertIn("mrs", out)

    def test_get_issues_help(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            cfg = self._write_min_config(pathlib.Path(td))
            proc = self._run_relman(["get", "issues", "--help"], config_path=cfg)

        self.assertEqual(proc.returncode, 0, msg=_debug_proc(proc))
        out = _strip_ansi(proc.stdout)
        self.assertIn("Usage: relman.py get issues", out)
        self.assertIn("MR_URL", out)
        self.assertIn("Ссылка на MR в GitLab", out)
        self.assertIn("--jira-project", out)
        self.assertIn("--gitlab-url-override", out)

    def test_get_mrs_help(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            cfg = self._write_min_config(pathlib.Path(td))
            proc = self._run_relman(["get", "mrs", "--help"], config_path=cfg)

        self.assertEqual(proc.returncode, 0, msg=_debug_proc(proc))
        out = _strip_ansi(proc.stdout)
        self.assertIn("Usage: relman.py get mrs", out)
        self.assertIn("--batch", out)
        self.assertIn("--target", out)
        self.assertIn("Получить список открытых", out)

    def test_create_help(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            cfg = self._write_min_config(pathlib.Path(td))
            proc = self._run_relman(["create", "--help"], config_path=cfg)

        self.assertEqual(proc.returncode, 0, msg=_debug_proc(proc))
        out = _strip_ansi(proc.stdout)
        self.assertIn("Usage: relman.py create", out)
        self.assertIn("Commands", out)
        self.assertIn("release", out)
        self.assertIn("mr", out)

    def test_create_release_help(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            cfg = self._write_min_config(pathlib.Path(td))
            proc = self._run_relman(["create", "release", "--help"], config_path=cfg)

        self.assertEqual(proc.returncode, 0, msg=_debug_proc(proc))
        out = _strip_ansi(proc.stdout)
        self.assertIn("Usage: relman.py create release", out)
        self.assertIn("MR_URL", out)
        self.assertIn("--jira-project", out)
        self.assertIn("--gitlab-tag", out)
        self.assertIn("Создать релиз", out)

    def test_create_mr_help(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            cfg = self._write_min_config(pathlib.Path(td))
            proc = self._run_relman(["create", "mr", "--help"], config_path=cfg)

        self.assertEqual(proc.returncode, 0, msg=_debug_proc(proc))
        out = _strip_ansi(proc.stdout)
        self.assertIn("Usage: relman.py create mr", out)
        self.assertIn("REPO_URL", out)
        self.assertIn("--from", out)
        self.assertIn("--to", out)
        self.assertIn("--batch", out)
        self.assertIn("--target", out)
        self.assertIn("--dry-run", out)


if __name__ == "__main__":
    unittest.main(verbosity=2)
