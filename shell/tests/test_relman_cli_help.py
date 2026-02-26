import os
import pathlib
import re
import subprocess
import sys
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

    def _run_relman(self, args: list[str]) -> subprocess.CompletedProcess[str]:
        cmd = [sys.executable, str(self.relman_py)]
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
        # Не проверяем справку по каждой подкоманде: это слишком хрупко и не даёт
        # реальной ценности. Этот smoke-тест гарантирует, что CLI стартует и
        # выводит справку без падений.


if __name__ == "__main__":
    unittest.main(verbosity=2)
