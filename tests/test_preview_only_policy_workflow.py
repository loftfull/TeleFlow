import unittest
from pathlib import Path


class PreviewOnlyPolicyWorkflowTests(unittest.TestCase):
    def test_secret_free_policy_runs_for_protected_branch_paths(self):
        root = Path(__file__).resolve().parents[1]
        path = root / ".github/workflows/preview-only-policy.yml"
        self.assertTrue(path.is_file(), path)
        text = path.read_text(encoding="utf-8")

        for token in [
            "pull_request:",
            "push:",
            "English",
            "teleflow/bootstrap",
            "permissions:\n  contents: read",
            "concurrency:",
            "cancel-in-progress: true",
            "python3 -m unittest tests.test_preview_only_policy tests.test_preview_only_policy_workflow -v",
            "python3 scripts/preview_only_policy.py",
            "uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683",
        ]:
            self.assertIn(token, text)

        self.assertNotIn("${{ secrets.", text)
        self.assertNotIn("upload-artifact", text)


if __name__ == "__main__":
    unittest.main()
