import unittest
from pathlib import Path


class SmartFoldersBatchApplyContractTests(unittest.TestCase):
    def test_batch_apply_preflights_conflicts_limits_and_uses_transactional_operations(self):
        root = Path(__file__).resolve().parents[1]
        path = root / "overlay/java/org/telegram/teleflow/integration/SmartFoldersBatchApply.java"
        self.assertTrue(path.is_file(), path)
        text = path.read_text()
        for token in [
            "ManagedFolderResolver.resolve",
            "FolderProvisioningPlanner.plan",
            "IDENTITY_MISMATCH",
            "ManagedFolderResolution.Status.MISSING",
            "blockedByLimit",
            "SmartFoldersBatchCoordinator.apply",
            "SmartFoldersTransaction",
            "ManagedFolderProvisioner.provision",
        ]:
            self.assertIn(token, text)
        preflight = text.index("blockedByLimit")
        batch = text.index("SmartFoldersBatchCoordinator.apply")
        self.assertLess(preflight, batch)


if __name__ == '__main__':
    unittest.main()
