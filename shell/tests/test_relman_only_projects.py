import importlib.util
import pathlib
import unittest


_REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
_RELMAN_PY = _REPO_ROOT / "shell" / "relman.py"
_spec = importlib.util.spec_from_file_location("relman", _RELMAN_PY)
if _spec is None or _spec.loader is None:
    raise RuntimeError(f"Cannot import relman from: {_RELMAN_PY}")
relman = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(relman)


class RelmanOnlyProjectsTest(unittest.TestCase):
    def test_parse_only_projects_csv_strips_and_skips_empty(self) -> None:
        self.assertEqual(
            relman.parse_only_projects_csv(" mm-core-bff, api-graphql , ,mm-geo-service ,"),
            ["mm-core-bff", "api-graphql", "mm-geo-service"],
        )

    def test_filter_projects_by_only_projects_matches_name_and_id(self) -> None:
        projects = [
            {"id": "mm-core-bff", "name": "mm-core-bff"},
            {"id": "api-graphql", "name": "api-graphql"},
            {"id": "mm-geo-service", "name": "mm-geo-service"},
        ]

        filtered = relman.filter_projects_by_only_projects(projects, ["api-graphql", "mm-geo-service"])
        self.assertEqual([p["id"] for p in filtered], ["api-graphql", "mm-geo-service"])

        filtered_by_id = relman.filter_projects_by_only_projects(projects, ["mm-core-bff"])
        self.assertEqual([p["name"] for p in filtered_by_id], ["mm-core-bff"])

    def test_filter_projects_by_only_projects_unknown_raises(self) -> None:
        projects = [
            {"id": "mm-core-bff", "name": "mm-core-bff"},
            {"id": "api-graphql", "name": "api-graphql"},
        ]
        with self.assertRaises(ValueError):
            relman.filter_projects_by_only_projects(projects, ["mm-geo-service"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
