import unittest

from relman import resolve_repo_url_from_config, select_projects_for_batch


class TestRelmanConfigResolution(unittest.TestCase):
    def setUp(self) -> None:
        self.cfg = {
            "projects": [
                {
                    "id": "api-graphql-id",
                    "name": "api-graphql",
                    "repo_url": "https://gitlab.example.com/group/api-graphql",
                },
                {
                    "id": "api-payment-service",
                    "name": "api-payment-service",
                    "repo_url": "https://gitlab.example.com/group/api-payment-service",
                },
            ]
        }

    def test_resolve_repo_url_keeps_http_url(self) -> None:
        self.assertEqual(
            resolve_repo_url_from_config(self.cfg, "https://gitlab.example.com/x/y"),
            "https://gitlab.example.com/x/y",
        )

    def test_resolve_repo_url_by_name(self) -> None:
        self.assertEqual(
            resolve_repo_url_from_config(self.cfg, "api-graphql"),
            "https://gitlab.example.com/group/api-graphql",
        )

    def test_resolve_repo_url_by_id(self) -> None:
        self.assertEqual(
            resolve_repo_url_from_config(self.cfg, "api-graphql-id"),
            "https://gitlab.example.com/group/api-graphql",
        )

    def test_resolve_repo_url_unknown_name_raises(self) -> None:
        with self.assertRaises(ValueError):
            resolve_repo_url_from_config(self.cfg, "unknown-project")

    def test_select_projects_for_batch_none_returns_all(self) -> None:
        projs = select_projects_for_batch(self.cfg, None)
        self.assertEqual(len(projs), 2)

    def test_select_projects_for_batch_by_name(self) -> None:
        projs = select_projects_for_batch(self.cfg, "api-payment-service")
        self.assertEqual(len(projs), 1)
        self.assertEqual(projs[0]["name"], "api-payment-service")

    def test_select_projects_for_batch_by_repo_url(self) -> None:
        projs = select_projects_for_batch(
            self.cfg, "https://gitlab.example.com/group/api-graphql"
        )
        self.assertEqual(len(projs), 1)
        self.assertEqual(projs[0]["name"], "api-graphql")

    def test_select_projects_for_batch_unknown_raises(self) -> None:
        with self.assertRaises(ValueError):
            select_projects_for_batch(self.cfg, "https://gitlab.example.com/unknown")


class TestDuplicates(unittest.TestCase):
    def test_duplicate_name_raises(self) -> None:
        cfg = {
            "projects": [
                {"id": "a", "name": "dup", "repo_url": "https://x/a"},
                {"id": "b", "name": "dup", "repo_url": "https://x/b"},
            ]
        }
        with self.assertRaises(ValueError):
            resolve_repo_url_from_config(cfg, "dup")


if __name__ == "__main__":
    unittest.main(verbosity=2)
