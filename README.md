# TeleFlow Android Bootstrap

TeleFlow is an unofficial Telegram-compatible Android client project built as a deterministic extension layer over the official Telegram Android source.

This repository does **not** vendor the full Telegram source tree. GitHub Actions checks out a pinned official Telegram commit, validates it, applies the TeleFlow overlay, and builds an Android APK.

## Current v0.1 bootstrap

Implemented in this bootstrap:

- product/application identity: `TeleFlow`, `com.loftfull.teleflow`;
- fail-closed validation of the pinned Telegram Android source;
- user-owned Telegram API credential injection;
- fork-safe disabling of official-only passkey/billing assumptions;
- isolated extension namespace: `org.telegram.teleflow`;
- bootstrap feature registry for Smart Folders and TeleFlow Drive;
- GitHub Actions pipeline for `afatDebug` APK artifacts;
- unit tests for source verification and deterministic patching.

This is the build foundation. Smart Folders UI, Telegram server folder mutation, Drive browser/transfer UI, Automation Center and AI are subsequent vertical slices.

## Required private configuration

Never put Telegram credentials into source files, issues, chat messages, or commits.

Configure these GitHub repository secrets:

- `TELEGRAM_API_ID`
- `TELEGRAM_API_HASH`

Obtain them from Telegram's **API development tools** page for your own application.

## Build

The workflow `.github/workflows/build-apk.yml`:

1. checks out this bootstrap;
2. checks out the pinned official Telegram Android source and submodules;
3. installs Android SDK 35, Build Tools 35.0.0, NDK 27.2.12479018 and CMake;
4. validates the Telegram source shape;
5. applies the TeleFlow overlay;
6. builds `:TMessagesProj_App:assembleAfatDebug`;
7. uploads the generated `app.apk` as the `teleflow-android-debug` artifact.

## Local self-check

```bash
bash scripts/selfcheck.sh
```

The self-check validates the bootstrap itself. A full Android build requires the Android toolchain and the official Telegram source checkout, so GitHub Actions is the canonical build environment for this bootstrap.

## Licensing

TeleFlow is designed as a derivative of Telegram Android and must be distributed in compliance with the upstream GPL terms. The TeleFlow-specific source in this bootstrap is intended to remain available under GPL-2.0-or-later compatible terms.

TeleFlow is unofficial and is not affiliated with Telegram.
