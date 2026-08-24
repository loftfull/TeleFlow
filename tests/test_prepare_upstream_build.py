import tempfile
import unittest
from pathlib import Path

from scripts.prepare_upstream_build import PatchError, prepare


SETTINGS = """include ':TMessagesProj'\ninclude ':TMessagesProj_App'\ninclude ':TMessagesProj_AppHuawei'\ninclude ':TMessagesProj_AppHockeyApp'\ninclude ':TMessagesProj_AppStandalone'\ninclude ':TMessagesProj_AppTests'\n"""


class PrepareUpstreamBuildTests(unittest.TestCase):
    def make_checkout(self):
        root = Path(tempfile.mkdtemp())
        (root / "settings.gradle").write_text(SETTINGS, encoding="utf-8")
        module = root / "TMessagesProj/lib/jlatexmath/jlatexmath"
        module.mkdir(parents=True)
        (module / "build.gradle").write_text("apply plugin: 'com.android.library'\n", encoding="utf-8")
        return root

    def test_adds_missing_jlatexmath_project_mapping(self):
        root = self.make_checkout()
        prepare(root)
        text = (root / "settings.gradle").read_text(encoding="utf-8")
        self.assertIn("include ':jlatexmath'", text)
        self.assertIn("project(':jlatexmath').projectDir = file('TMessagesProj/lib/jlatexmath/jlatexmath')", text)
        self.assertEqual(text.count("include ':jlatexmath'"), 1)

    def test_fails_closed_if_expected_module_is_missing(self):
        root = self.make_checkout()
        (root / "TMessagesProj/lib/jlatexmath/jlatexmath/build.gradle").unlink()
        with self.assertRaises(PatchError):
            prepare(root)

    def test_is_idempotent(self):
        root = self.make_checkout()
        prepare(root)
        prepare(root)
        text = (root / "settings.gradle").read_text(encoding="utf-8")
        self.assertEqual(text.count("include ':jlatexmath'"), 1)


if __name__ == "__main__":
    unittest.main()
