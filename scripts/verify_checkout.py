#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path

EXPECTED_VERSION = "12.10.0"
EXPECTED_PACKAGE = "org.telegram.messenger"
EXPECTED_GRADLE = "8.11.1"


class VerificationError(RuntimeError):
    pass


@dataclass(frozen=True)
class CheckoutReport:
    version_name: str
    package_name: str
    gradle_version: str


def _read(path: Path) -> str:
    if not path.is_file():
        raise VerificationError(f"required upstream file is missing: {path}")
    return path.read_text(encoding="utf-8")


def _property(text: str, key: str) -> str:
    match = re.search(rf"(?m)^{re.escape(key)}=(.+)$", text)
    if not match:
        raise VerificationError(f"required Gradle property is missing: {key}")
    return match.group(1).strip()


def verify_checkout(root: Path) -> CheckoutReport:
    root = Path(root)
    props = _read(root / "gradle.properties")
    version = _property(props, "APP_VERSION_NAME")
    package = _property(props, "APP_PACKAGE")
    if version != EXPECTED_VERSION:
        raise VerificationError(f"unexpected Telegram version: {version}; expected {EXPECTED_VERSION}")
    if package != EXPECTED_PACKAGE:
        raise VerificationError(f"checkout is not pristine Telegram package: {package}")

    buildvars = _read(root / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java")
    for anchor in (
        "public static int APP_ID = 4;",
        'public static String APP_HASH = "014b35b6184100b085b0d0572f9b5103";',
    ):
        if buildvars.count(anchor) != 1:
            raise VerificationError(f"unexpected BuildVars anchor: {anchor}")

    strings = _read(root / "TMessagesProj/src/main/res/values/strings.xml")
    if strings.count('<string name="AppName">Telegram</string>') != 1:
        raise VerificationError("unexpected upstream AppName anchor")

    wrapper = _read(root / "gradle/wrapper/gradle-wrapper.properties")
    match = re.search(r"gradle-([0-9.]+)-bin\.zip", wrapper)
    if not match:
        raise VerificationError("unable to determine upstream Gradle wrapper version")
    gradle = match.group(1)
    if gradle != EXPECTED_GRADLE:
        raise VerificationError(f"unexpected Gradle version: {gradle}; expected {EXPECTED_GRADLE}")

    return CheckoutReport(version_name=version, package_name=package, gradle_version=gradle)


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify the pinned official Telegram checkout before TeleFlow patching")
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    try:
        report = verify_checkout(args.root)
    except VerificationError as exc:
        parser.error(str(exc))
    print(f"Telegram checkout verified: v{report.version_name}, Gradle {report.gradle_version}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
