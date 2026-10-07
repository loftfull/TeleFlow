import unittest
from pathlib import Path


class ManagedFolderResolverContractTests(unittest.TestCase):
    def test_resolver_uses_live_id_name_and_dynamic_telegram_limits(self):
        root = Path(__file__).resolve().parents[1]
        path = root / "overlay/java/org/telegram/teleflow/integration/ManagedFolderResolver.java"
        self.assertTrue(path.is_file(), path)
        text = path.read_text()
        for token in [
            "ManagedFolderIdentityGuard.resolve",
            "loadRegistration",
            "getDialogFilters()",
            "dialogFiltersLimitDefault",
            "dialogFiltersLimitPremium",
            "UserConfig.getInstance(account).isPremium()",
            "IDENTITY_MISMATCH",
        ]:
            self.assertIn(token, text)
        self.assertNotIn("filter.name.equals(spec.getDisplayName())", text)


if __name__ == '__main__':
    unittest.main()
