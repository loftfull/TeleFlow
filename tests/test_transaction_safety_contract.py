import unittest
from pathlib import Path


class TransactionSafetyContractTests(unittest.TestCase):
    def test_critical_state_stores_use_synchronous_commit(self):
        root = Path(__file__).resolve().parents[1]
        for relative in [
            "overlay/java/org/telegram/teleflow/integration/FolderBackupStore.java",
            "overlay/java/org/telegram/teleflow/integration/ManagedFolderStateStore.java",
        ]:
            text = (root / relative).read_text()
            self.assertIn(".commit()", text, relative)
            self.assertNotIn(".apply();", text, relative)
            self.assertIn("public boolean save", text, relative)
            self.assertIn("public boolean delete", text, relative)

    def test_coordinator_refuses_remote_mutation_without_backup(self):
        root = Path(__file__).resolve().parents[1]
        text = (root / "overlay/java/org/telegram/teleflow/folders/FolderApplyCoordinator.java").read_text()
        backup_pos = text.index("if (!backup.save(current))")
        remote_pos = text.index("remote.apply(plan")
        self.assertLess(backup_pos, remote_pos)
        self.assertIn("remote.restore(current", text)
        self.assertIn("managed-state-save-failed-rolled-back", text)

    def test_gateway_refuses_unresolved_peers_and_empty_responses(self):
        root = Path(__file__).resolve().parents[1]
        text = (root / "overlay/java/org/telegram/teleflow/integration/TelegramFolderGateway.java").read_text()
        self.assertIn("unresolved-dialog:", text)
        self.assertIn("telegram-empty-response", text)
        self.assertIn("if (user == null)", text)
        self.assertIn("if (chat == null)", text)
        self.assertIn("if (DialogObject.isEncryptedDialog(did))", text)


if __name__ == "__main__":
    unittest.main()
