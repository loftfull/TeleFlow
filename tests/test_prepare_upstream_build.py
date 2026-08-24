import tempfile
import unittest
from pathlib import Path

from scripts.prepare_upstream_build import PatchError, prepare


SETTINGS = """include ':TMessagesProj'\ninclude ':TMessagesProj_App'\ninclude ':TMessagesProj_AppHuawei'\ninclude ':TMessagesProj_AppHockeyApp'\ninclude ':TMessagesProj_AppStandalone'\ninclude ':TMessagesProj_AppTests'\n"""
APP_BUILD = """apply plugin: 'com.android.application'\n\nandroid {\n}\n\napply plugin: 'com.google.gms.google-services'\n"""


class PrepareUpstreamBuildTests(unittest.TestCase):
    def make_checkout(self):
        root = Path(tempfile.mkdtemp())
        (root / "settings.gradle").write_text(SETTINGS, encoding="utf-8")
        module = root / "TMessagesProj/lib/jlatexmath/jlatexmath"
        module.mkdir(parents=True)
        (module / "build.gradle").write_text("apply plugin: 'com.android.library'\n", encoding="utf-8")
        app = root / "TMessagesProj_App"
        app.mkdir(parents=True)
        (app / "build.gradle").write_text(APP_BUILD, encoding="utf-8")
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
        settings = (root / "settings.gradle").read_text(encoding="utf-8")
        app_build = (root / "TMessagesProj_App/build.gradle").read_text(encoding="utf-8")
        self.assertEqual(settings.count("include ':jlatexmath'"), 1)
        self.assertEqual(app_build.count("TeleFlow: Google services disabled until a TeleFlow Firebase config is supplied"), 1)

    def test_disables_official_google_services_plugin_for_rebranded_build(self):
        root = self.make_checkout()
        prepare(root)
        text = (root / "TMessagesProj_App/build.gradle").read_text(encoding="utf-8")
        self.assertNotIn("apply plugin: 'com.google.gms.google-services'", text)
        self.assertIn("TeleFlow: Google services disabled until a TeleFlow Firebase config is supplied", text)

    def test_fails_closed_if_google_services_plugin_shape_changes(self):
        root = self.make_checkout()
        (root / "TMessagesProj_App/build.gradle").write_text("apply plugin: 'com.android.application'\n", encoding="utf-8")
        with self.assertRaises(PatchError):
            prepare(root)


if __name__ == "__main__":
    unittest.main()
