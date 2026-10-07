import importlib
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
POLICY_MODULE = ROOT / "scripts/preview_only_policy.py"


def load_policy():
    if not POLICY_MODULE.is_file():
        return None
    importlib.invalidate_caches()
    return importlib.import_module("scripts.preview_only_policy")


class PreviewOnlyPolicyTests(unittest.TestCase):
    def test_policy_module_exists_and_current_repository_is_clean(self):
        self.assertTrue(POLICY_MODULE.is_file(), POLICY_MODULE)
        policy = load_policy()
        violations = policy.scan_repository(ROOT)
        self.assertEqual([], violations, "\n".join(str(item) for item in violations))

    def test_scanner_rejects_whitespace_and_unqualified_bypasses(self):
        self.assertTrue(POLICY_MODULE.is_file(), POLICY_MODULE)
        policy = load_policy()
        source = """
            fragment.getConnectionsManager().sendRequest (request, callback);
            saveFilterToServer(fragment, filter);
        """
        violations = policy.scan_shipping_source("overlay/java/example/Bypass.java", source)
        rules = {item.rule for item in violations}
        self.assertIn("direct-network-request", rules)
        self.assertIn("telegram-filter-helper", rules)

    def test_scanner_rejects_method_reference_and_comment_split_sink(self):
        self.assertTrue(POLICY_MODULE.is_file(), POLICY_MODULE)
        policy = load_policy()
        source = """
            callback = manager::sendRequest;
            manager.sendRequest/**/(request, callback);
        """
        violations = policy.scan_shipping_source("overlay/java/example/Bypass.java", source)
        requests = [item for item in violations if item.rule == "direct-network-request"]
        self.assertEqual(2, len(requests))

    def test_scanner_rejects_sink_in_patcher_generated_java(self):
        self.assertTrue(POLICY_MODULE.is_file(), POLICY_MODULE)
        policy = load_policy()
        source = "generated = r'''gateway.sendRequest (request);'''"
        violations = policy.scan_shipping_source("scripts/apply_teleflow.py", source)
        self.assertTrue(any(item.rule == "direct-network-request" for item in violations))

    def test_gateway_rejects_a_third_request_even_when_disabled_constant_remains(self):
        self.assertTrue(POLICY_MODULE.is_file(), POLICY_MODULE)
        policy = load_policy()
        gateway = (ROOT / policy.GATEWAY_PATH).read_text(encoding="utf-8")
        gateway += "\nvoid bypass() { manager.sendRequest (request); }\n"
        violations = policy.scan_gateway(gateway)
        self.assertTrue(any(item.rule == "gateway-request-count" for item in violations))

    def test_gateway_rejects_missing_return_in_each_disabled_guard(self):
        self.assertTrue(POLICY_MODULE.is_file(), POLICY_MODULE)
        policy = load_policy()
        gateway = (ROOT / policy.GATEWAY_PATH).read_text(encoding="utf-8")
        protected = 'callback.onResult(false, "mutations-disabled");\n            return;'
        unprotected = 'callback.onResult(false, "mutations-disabled");'
        self.assertEqual(2, gateway.count(protected))

        for occurrence in range(2):
            start = -1
            for _ in range(occurrence + 1):
                start = gateway.index(protected, start + 1)
            mutated = gateway[:start] + gateway[start:].replace(protected, unprotected, 1)
            violations = policy.scan_gateway(mutated)
            self.assertTrue(
                any(item.rule == "gateway-unguarded-request" for item in violations),
                f"missing return in guard {occurrence + 1} was not rejected",
            )

    def test_gateway_rejects_local_kill_switch_shadow(self):
        self.assertTrue(POLICY_MODULE.is_file(), POLICY_MODULE)
        policy = load_policy()
        gateway = (ROOT / policy.GATEWAY_PATH).read_text(encoding="utf-8")
        shadowed = gateway.replace(
            "if (!MUTATIONS_ENABLED) {",
            "boolean MUTATIONS_ENABLED = true;\n        if (!MUTATIONS_ENABLED) {",
            1,
        )
        violations = policy.scan_gateway(shadowed)
        self.assertTrue(any(item.rule == "gateway-switch-usage-count" for item in violations))

    def test_gateway_seal_rejects_control_flow_bypass(self):
        self.assertTrue(POLICY_MODULE.is_file(), POLICY_MODULE)
        policy = load_policy()
        gateway = (ROOT / policy.GATEWAY_PATH).read_text(encoding="utf-8")
        guard = """if (!MUTATIONS_ENABLED) {
            callback.onResult(false, \"mutations-disabled\");
            return;
        }"""
        nested = """if (false) {
        if (!MUTATIONS_ENABLED) {
            callback.onResult(false, \"mutations-disabled\");
            return;
        }
        }"""
        bypassed = gateway.replace(guard, nested, 1)
        self.assertNotEqual(gateway, bypassed)

        violations = policy.scan_gateway(bypassed)
        self.assertTrue(any(item.rule == "gateway-seal" for item in violations))

    def test_repository_scan_discovers_nested_overlay_file(self):
        self.assertTrue(POLICY_MODULE.is_file(), POLICY_MODULE)
        policy = load_policy()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            nested = root / "overlay/java/example/deep/Bypass.java"
            nested.parent.mkdir(parents=True)
            nested.write_text("class Bypass { void run() { manager.sendRequest (request); } }", encoding="utf-8")
            patcher = root / "scripts/apply_teleflow.py"
            patcher.parent.mkdir(parents=True)
            patcher.write_text("# safe fixture\n", encoding="utf-8")

            violations = policy.scan_repository(root)
            self.assertTrue(
                any(item.path.endswith("example/deep/Bypass.java") and item.rule == "direct-network-request" for item in violations)
            )

    def test_repository_scan_rejects_missing_gateway(self):
        self.assertTrue(POLICY_MODULE.is_file(), POLICY_MODULE)
        policy = load_policy()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            overlay = root / "overlay/java"
            overlay.mkdir(parents=True)
            patcher = root / "scripts/apply_teleflow.py"
            patcher.parent.mkdir(parents=True)
            patcher.write_text("# safe fixture\n", encoding="utf-8")

            violations = policy.scan_repository(root)
            self.assertTrue(any(item.rule == "gateway-missing" for item in violations))


if __name__ == "__main__":
    unittest.main()
