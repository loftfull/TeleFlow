import unittest
from pathlib import Path


class FolderProvisionerContractTests(unittest.TestCase):
    def test_provisioner_creates_server_folder_then_registers_or_rolls_back(self):
        root = Path(__file__).resolve().parents[1]
        provisioner = root / "overlay/java/org/telegram/teleflow/integration/ManagedFolderProvisioner.java"
        gateway = root / "overlay/java/org/telegram/teleflow/integration/TelegramFolderGateway.java"
        self.assertTrue(provisioner.is_file(), provisioner)
        self.assertTrue(gateway.is_file(), gateway)
        p = provisioner.read_text()
        g = gateway.read_text()
        for token in [
            "createNew",
            "saveRegistration",
            "deleteFolder",
            "ManagedFolderRegistration",
            "registry-save-failed-rolled-back",
        ]:
            self.assertIn(token, p)
        managed_failure = p.index("if (!managed.save(state))")
        rollback_delete = p.index("TelegramFolderGateway.deleteFolder", managed_failure)
        registry_delete = p.index("registry.delete(spec.getLogicalKey())", managed_failure)
        self.assertLess(rollback_delete, registry_delete, "ownership must survive until remote delete succeeds")
        for token in [
            "allocateFilterId",
            "controller.addFilter(filter, false)",
            "TL_messages_updateDialogFilter",
            "deleteFolder",
        ]:
            self.assertIn(token, g)


if __name__ == '__main__':
    unittest.main()
