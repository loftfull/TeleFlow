#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path


class PatchError(RuntimeError):
    pass


INCLUDE_LINE = "include ':jlatexmath'"
PROJECT_LINE = "project(':jlatexmath').projectDir = file('TMessagesProj/lib/jlatexmath/jlatexmath')"
ANCHOR = "include ':TMessagesProj_AppTests'\n"
GOOGLE_SERVICES_PLUGIN = "apply plugin: 'com.google.gms.google-services'"
GOOGLE_SERVICES_DISABLED = "// TeleFlow: Google services disabled until a TeleFlow Firebase config is supplied"
AFAT_FULL_ABI = '''        afat {
            ndk {
                abiFilters "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
            }
'''
AFAT_ARM64_ABI = '''        afat {
            ndk {
                abiFilters "arm64-v8a"
            }
'''


def prepare(root: Path) -> None:
    root = Path(root)
    settings = root / "settings.gradle"
    module_build = root / "TMessagesProj/lib/jlatexmath/jlatexmath/build.gradle"
    app_build = root / "TMessagesProj_App/build.gradle"
    if not settings.is_file():
        raise PatchError(f"required upstream file is missing: {settings}")
    if not module_build.is_file():
        raise PatchError(f"jlatexmath submodule is missing: {module_build}")
    if not app_build.is_file():
        raise PatchError(f"required upstream file is missing: {app_build}")

    text = settings.read_text(encoding="utf-8")
    has_include = INCLUDE_LINE in text
    has_project = PROJECT_LINE in text
    if has_include != has_project:
        raise PatchError("partial jlatexmath Gradle mapping found; refusing to guess")
    if not has_include:
        if text.count(ANCHOR) != 1:
            raise PatchError("unexpected Telegram settings.gradle shape")
        replacement = ANCHOR + INCLUDE_LINE + "\n" + PROJECT_LINE + "\n"
        settings.write_text(text.replace(ANCHOR, replacement), encoding="utf-8")

    app_text = app_build.read_text(encoding="utf-8")
    has_plugin = GOOGLE_SERVICES_PLUGIN in app_text
    has_disabled_marker = GOOGLE_SERVICES_DISABLED in app_text
    if has_plugin and has_disabled_marker:
        raise PatchError("partial Google Services compatibility patch found; refusing to guess")
    if not has_plugin and not has_disabled_marker:
        raise PatchError("unexpected Telegram TMessagesProj_App/build.gradle Google Services shape")
    if has_plugin:
        if app_text.count(GOOGLE_SERVICES_PLUGIN) != 1:
            raise PatchError("unexpected Google Services plugin occurrence count")
        app_build.write_text(app_text.replace(GOOGLE_SERVICES_PLUGIN, GOOGLE_SERVICES_DISABLED), encoding="utf-8")


def prepare_arm64_smoke(root: Path) -> None:
    root = Path(root)
    app_build = root / "TMessagesProj_App/build.gradle"
    if not app_build.is_file():
        raise PatchError(f"required upstream file is missing: {app_build}")

    text = app_build.read_text(encoding="utf-8")
    has_full = AFAT_FULL_ABI in text
    has_arm64 = AFAT_ARM64_ABI in text
    if has_full and has_arm64:
        raise PatchError("partial arm64 smoke ABI patch found; refusing to guess")
    if has_arm64:
        return
    if text.count(AFAT_FULL_ABI) != 1:
        raise PatchError("unexpected Telegram afat ABI shape")
    app_build.write_text(text.replace(AFAT_FULL_ABI, AFAT_ARM64_ABI), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Prepare pinned Telegram checkout for reproducible Gradle build")
    parser.add_argument("root", type=Path)
    parser.add_argument("--arm64-smoke", action="store_true", help="restrict only the afat flavor to arm64-v8a for fast smoke builds")
    args = parser.parse_args()
    try:
        prepare(args.root)
        if args.arm64_smoke:
            prepare_arm64_smoke(args.root)
    except PatchError as exc:
        parser.error(str(exc))
    suffix = ", afat arm64 smoke enabled" if args.arm64_smoke else ""
    print("Telegram Gradle compatibility prepared: jlatexmath mapped, official Google Services disabled" + suffix)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
