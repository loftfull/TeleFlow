#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import re
import sys
from dataclasses import dataclass
from pathlib import Path


GATEWAY_PATH = Path("overlay/java/org/telegram/teleflow/integration/TelegramFolderGateway.java")
PATCHER_PATH = Path("scripts/apply_teleflow.py")
GATEWAY_SHA256 = "5b00bbef8b6233882e7eb15b3502771a207226ba3a0da3497d5474340b847384"

SINK_PATTERNS = {
    "direct-network-request": re.compile(r"\bsendRequest\b"),
    "telegram-filter-rpc": re.compile(r"\bTL_messages_updateDialogFilter\b"),
    "telegram-filter-helper": re.compile(r"\bsaveFilterToServer\b"),
    "local-filter-save": re.compile(r"\bsaveDialogFilter\b"),
    "local-filter-delete": re.compile(r"\bdeleteDialogFilter\b"),
    "local-filter-add": re.compile(r"\baddFilter\b"),
    "local-filter-remove": re.compile(r"\bremoveFilter\b"),
}

GATEWAY_EXPECTED_COUNTS = {
    "direct-network-request": 2,
    "telegram-filter-rpc": 4,
    "telegram-filter-helper": 0,
    "local-filter-save": 1,
    "local-filter-delete": 1,
    "local-filter-add": 1,
    "local-filter-remove": 1,
}


@dataclass(frozen=True)
class Violation:
    path: str
    line: int
    rule: str
    detail: str

    def __str__(self) -> str:
        return f"{self.path}:{self.line}: {self.rule}: {self.detail}"


def _line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def _violation(path: str, text: str, match: re.Match[str], rule: str) -> Violation:
    return Violation(path, _line_number(text, match.start()), rule, match.group(0).strip())


def scan_shipping_source(path: str | Path, text: str) -> list[Violation]:
    relative = Path(path).as_posix()
    violations: list[Violation] = []
    for rule, pattern in SINK_PATTERNS.items():
        violations.extend(_violation(relative, text, match, rule) for match in pattern.finditer(text))
    return violations


def _method_body(text: str, signature: re.Pattern[str]) -> tuple[int, str] | None:
    method = signature.search(text)
    if method is None:
        return None
    opening = text.find("{", method.end())
    if opening < 0:
        return None
    depth = 0
    for index in range(opening, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return opening + 1, text[opening + 1:index]
    return None


def scan_gateway(text: str) -> list[Violation]:
    path = GATEWAY_PATH.as_posix()
    violations: list[Violation] = []
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    digest = hashlib.sha256(normalized.encode("utf-8")).hexdigest()
    if digest != GATEWAY_SHA256:
        violations.append(Violation(path, 1, "gateway-seal", f"expected {GATEWAY_SHA256}, found {digest}"))
    disabled = re.compile(
        r"private\s+static\s+final\s+boolean\s+MUTATIONS_ENABLED\s*=\s*false\s*;"
    )
    declarations = list(disabled.finditer(text))
    if len(declarations) != 1:
        violations.append(Violation(path, 1, "gateway-disabled-constant", f"expected 1 disabled declaration, found {len(declarations)}"))
    switch_uses = list(re.finditer(r"\bMUTATIONS_ENABLED\b", text))
    if len(switch_uses) != 3:
        violations.append(Violation(path, 1, "gateway-switch-usage-count", f"expected 3 uses, found {len(switch_uses)}"))

    for rule, pattern in SINK_PATTERNS.items():
        count = len(list(pattern.finditer(text)))
        expected = GATEWAY_EXPECTED_COUNTS[rule]
        if count != expected:
            line = _line_number(text, next(pattern.finditer(text)).start()) if count else 1
            name = "gateway-request-count" if rule == "direct-network-request" else "gateway-sink-count"
            violations.append(Violation(path, line, name, f"{rule}: expected {expected}, found {count}"))

    request_pattern = SINK_PATTERNS["direct-network-request"]
    guard_block = re.compile(
        r"if\s*\(\s*!\s*MUTATIONS_ENABLED\s*\)\s*\{\s*"
        r"callback\s*\.\s*onResult\s*\(\s*false\s*,\s*\"mutations-disabled\"\s*\)\s*;\s*"
        r"return\s*;\s*\}"
    )
    methods = {
        "deleteFolder": re.compile(r"public\s+static\s+void\s+deleteFolder\s*\("),
        "send": re.compile(r"private\s+static\s+void\s+send\s*\("),
    }
    for name, signature in methods.items():
        extracted = _method_body(text, signature)
        if extracted is None:
            violations.append(Violation(path, 1, "gateway-method-missing", name))
            continue
        offset, body = extracted
        requests = list(request_pattern.finditer(body))
        if len(requests) != 1:
            violations.append(Violation(path, _line_number(text, offset), "gateway-method-request-count", f"{name}: expected 1, found {len(requests)}"))
            continue
        before_request = body[:requests[0].start()]
        if guard_block.search(before_request) is None:
            violations.append(Violation(path, _line_number(text, offset + requests[0].start()), "gateway-unguarded-request", name))
    return violations


def scan_repository(root: Path) -> list[Violation]:
    root = Path(root)
    violations: list[Violation] = []
    overlay_root = root / "overlay"
    gateway = root / GATEWAY_PATH
    if not overlay_root.is_dir():
        violations.append(Violation("overlay", 1, "shipping-source-root-missing", "overlay directory is required"))
    if not gateway.is_file():
        violations.append(Violation(GATEWAY_PATH.as_posix(), 1, "gateway-missing", "disabled gateway is required"))
    if overlay_root.is_dir():
        for source in sorted(overlay_root.rglob("*.java")):
            relative = source.relative_to(root)
            text = source.read_text(encoding="utf-8")
            if relative == GATEWAY_PATH:
                violations.extend(scan_gateway(text))
            else:
                violations.extend(scan_shipping_source(relative, text))

    patcher = root / PATCHER_PATH
    if not patcher.is_file():
        violations.append(Violation(PATCHER_PATH.as_posix(), 1, "shipping-source-missing", "patcher is required"))
    else:
        violations.extend(scan_shipping_source(PATCHER_PATH, patcher.read_text(encoding="utf-8")))
    return violations


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    violations = scan_repository(root)
    if violations:
        print("Preview-only policy violations:", file=sys.stderr)
        for item in violations:
            print(item, file=sys.stderr)
        return 1
    print("Preview-only policy passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
