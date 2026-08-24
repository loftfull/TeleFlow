#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path


class PatchError(RuntimeError):
    pass


INCLUDE_LINE = "include ':jlatexmath'"
PROJECT_LINE = "project(':jlatexmath').projectDir = file('TMessagesProj/lib/jlatexmath/jlatexmath')"
ANCHOR = "include ':TMessagesProj_AppTests'\n"


def prepare(root: Path) -> None:
    root = Path(root)
    settings = root / "settings.gradle"
    module_build = root / "TMessagesProj/lib/jlatexmath/jlatexmath/build.gradle"
    if not settings.is_file():
        raise PatchError(f"required upstream file is missing: {settings}")
    if not module_build.is_file():
        raise PatchError(f"jlatexmath submodule is missing: {module_build}")

    text = settings.read_text(encoding="utf-8")
    has_include = INCLUDE_LINE in text
    has_project = PROJECT_LINE in text
    if has_include and has_project:
        return
    if has_include != has_project:
        raise PatchError("partial jlatexmath Gradle mapping found; refusing to guess")
    if text.count(ANCHOR) != 1:
        raise PatchError("unexpected Telegram settings.gradle shape")

    replacement = ANCHOR + INCLUDE_LINE + "\n" + PROJECT_LINE + "\n"
    settings.write_text(text.replace(ANCHOR, replacement), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Prepare pinned Telegram checkout for reproducible Gradle build")
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    try:
        prepare(args.root)
    except PatchError as exc:
        parser.error(str(exc))
    print("Telegram Gradle compatibility prepared: jlatexmath mapped")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
