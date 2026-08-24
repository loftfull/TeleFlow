import subprocess
import tempfile
import unittest
from pathlib import Path

from scripts.apply_teleflow import PatchError, apply_teleflow, validate_credentials


BUILDVARS = '''package org.telegram.messenger;\n\npublic class BuildVars {\n    public static int APP_ID = 4;\n    public static String APP_HASH = "014b35b6184100b085b0d0572f9b5103";\n    public static boolean IS_BILLING_UNAVAILABLE = false;\n    public static boolean SUPPORTS_PASSKEYS = true;\n}\n'''

GRADLE_PROPERTIES = '''APP_VERSION_CODE=7031\nAPP_VERSION_NAME=12.10.0\nAPP_PACKAGE=org.telegram.messenger\n'''

STRINGS = '''<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="AppName">Telegram</string>\n    <string name="AppNameBeta">Telegram Beta</string>\n</resources>\n'''


class TeleFlowPatcherTests(unittest.TestCase):
    def make_checkout(self) -> Path:
        root = Path(tempfile.mkdtemp())
        (root / "TMessagesProj/src/main/java/org/telegram/messenger").mkdir(parents=True)
        (root / "TMessagesProj/src/main/res/values").mkdir(parents=True)
        (root / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java").write_text(BUILDVARS)
        (root / "TMessagesProj/src/main/res/values/strings.xml").write_text(STRINGS)
        (root / "gradle.properties").write_text(GRADLE_PROPERTIES)
        return root

    def test_rejects_official_or_malformed_credentials(self):
        with self.assertRaises(PatchError):
            validate_credentials("4", "014b35b6184100b085b0d0572f9b5103")
        with self.assertRaises(PatchError):
            validate_credentials("abc", "f" * 32)
        with self.assertRaises(PatchError):
            validate_credentials("123456", "short")

    def test_rebrands_and_injects_credentials(self):
        root = self.make_checkout()
        report = apply_teleflow(root, "123456", "0123456789abcdef0123456789abcdef")

        buildvars = (root / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java").read_text()
        props = (root / "gradle.properties").read_text()
        strings = (root / "TMessagesProj/src/main/res/values/strings.xml").read_text()

        self.assertIn("public static int APP_ID = 123456;", buildvars)
        self.assertIn('public static String APP_HASH = "0123456789abcdef0123456789abcdef";', buildvars)
        self.assertIn("public static boolean IS_BILLING_UNAVAILABLE = true;", buildvars)
        self.assertIn("public static boolean SUPPORTS_PASSKEYS = false;", buildvars)
        self.assertIn("APP_PACKAGE=com.loftfull.teleflow", props)
        self.assertIn('<string name="AppName">TeleFlow</string>', strings)
        self.assertIn('<string name="AppNameBeta">TeleFlow Beta</string>', strings)
        self.assertGreaterEqual(report.changed_files, 10)

        features = root / "TMessagesProj/src/main/java/org/telegram/teleflow/core/TeleFlowFeatures.java"
        self.assertTrue(features.is_file())
        feature_text = features.read_text()
        self.assertIn("SMART_FOLDERS_BOOTSTRAP = true", feature_text)
        self.assertIn("DRIVE_BOOTSTRAP = true", feature_text)
        self.assertIn('PRODUCT_NAME = "TeleFlow"', feature_text)

        folders_dir = root / "TMessagesProj/src/main/java/org/telegram/teleflow/folders"
        drive_dir = root / "TMessagesProj/src/main/java/org/telegram/teleflow/drive"
        expected = [
            folders_dir / "ChatDescriptor.java",
            folders_dir / "FolderRule.java",
            folders_dir / "ClassificationDecision.java",
            folders_dir / "FolderRuleEngine.java",
            drive_dir / "VirtualPath.java",
            drive_dir / "DriveObject.java",
        ]
        for generated in expected:
            self.assertTrue(generated.is_file(), generated)

        harness = root / "TeleFlowDomainHarness.java"
        harness.write_text(r'''
import java.util.Arrays;
import org.telegram.teleflow.folders.ChatDescriptor;
import org.telegram.teleflow.folders.FolderRule;
import org.telegram.teleflow.folders.FolderRuleEngine;
import org.telegram.teleflow.folders.ClassificationDecision;
import org.telegram.teleflow.drive.VirtualPath;
import org.telegram.teleflow.drive.DriveObject;

public class TeleFlowDomainHarness {
    public static void main(String[] args) {
        FolderRule finance = new FolderRule(
            "Finance",
            ChatDescriptor.PeerType.CHANNEL,
            Arrays.asList("nasdaq", "investment")
        );
        FolderRuleEngine engine = new FolderRuleEngine(Arrays.asList(finance), "Review");
        ClassificationDecision hit = engine.classify(
            new ChatDescriptor(10L, "NASDAQ Investment News", "markets", ChatDescriptor.PeerType.CHANNEL)
        );
        if (!"Finance".equals(hit.getFolder()) || hit.getConfidence() != 1.0d) {
            throw new AssertionError("folder classification failed");
        }
        ClassificationDecision miss = engine.classify(
            new ChatDescriptor(11L, "Family", "family", ChatDescriptor.PeerType.GROUP)
        );
        if (!"Review".equals(miss.getFolder())) {
            throw new AssertionError("fallback classification failed");
        }
        String path = VirtualPath.normalize("Work//Contracts/2026/");
        if (!"/Work/Contracts/2026".equals(path)) {
            throw new AssertionError("virtual path normalization failed: " + path);
        }
        boolean rejected = false;
        try {
            VirtualPath.normalize("../secret");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        if (!rejected) {
            throw new AssertionError("parent traversal was accepted");
        }
        DriveObject object = new DriveObject(42, 99L, "contract.pdf", "application/pdf", 1024L, path);
        if (!"contract.pdf".equals(object.getName()) || !path.equals(object.getVirtualPath())) {
            throw new AssertionError("drive object failed");
        }
    }
}
''')
        java_sources = [str(path) for path in expected] + [str(harness)]
        classes = root / "classes"
        classes.mkdir()
        subprocess.run(["javac", "-d", str(classes), *java_sources], check=True)
        subprocess.run(["java", "-cp", str(classes), "TeleFlowDomainHarness"], check=True)

    def test_fails_closed_when_upstream_anchor_is_missing(self):
        root = self.make_checkout()
        buildvars = root / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"
        buildvars.write_text(BUILDVARS.replace("APP_ID = 4", "APP_ID = 999"))

        with self.assertRaises(PatchError):
            apply_teleflow(root, "123456", "0123456789abcdef0123456789abcdef")


if __name__ == "__main__":
    unittest.main()
