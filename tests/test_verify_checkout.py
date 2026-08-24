import tempfile
import unittest
from pathlib import Path

from scripts.verify_checkout import VerificationError, verify_checkout


class VerifyCheckoutTests(unittest.TestCase):
    def make_checkout(self, *, version="12.10.0", package="org.telegram.messenger") -> Path:
        root = Path(tempfile.mkdtemp())
        (root / "TMessagesProj/src/main/java/org/telegram/messenger").mkdir(parents=True)
        (root / "TMessagesProj/src/main/res/values").mkdir(parents=True)
        (root / "gradle/wrapper").mkdir(parents=True)
        (root / "gradle.properties").write_text(
            f"APP_VERSION_CODE=7031\nAPP_VERSION_NAME={version}\nAPP_PACKAGE={package}\n"
        )
        (root / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java").write_text(
            'public static int APP_ID = 4;\npublic static String APP_HASH = "014b35b6184100b085b0d0572f9b5103";\n'
        )
        (root / "TMessagesProj/src/main/res/values/strings.xml").write_text(
            '<string name="AppName">Telegram</string>\n<string name="AppNameBeta">Telegram Beta</string>\n'
        )
        (root / "gradle/wrapper/gradle-wrapper.properties").write_text(
            'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.11.1-bin.zip\n'
        )
        return root

    def test_accepts_pinned_clean_upstream_shape(self):
        report = verify_checkout(self.make_checkout())
        self.assertEqual(report.version_name, "12.10.0")
        self.assertEqual(report.package_name, "org.telegram.messenger")
        self.assertEqual(report.gradle_version, "8.11.1")

    def test_rejects_unexpected_upstream_version(self):
        with self.assertRaises(VerificationError):
            verify_checkout(self.make_checkout(version="99.0.0"))

    def test_rejects_already_rebranded_checkout(self):
        with self.assertRaises(VerificationError):
            verify_checkout(self.make_checkout(package="com.loftfull.teleflow"))


if __name__ == "__main__":
    unittest.main()
