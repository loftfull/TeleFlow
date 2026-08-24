#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path

OFFICIAL_APP_ID = "4"
OFFICIAL_APP_HASH = "014b35b6184100b085b0d0572f9b5103"


class PatchError(RuntimeError):
    pass


@dataclass(frozen=True)
class PatchReport:
    changed_files: int


def validate_credentials(api_id: str, api_hash: str) -> tuple[int, str]:
    api_id = api_id.strip()
    api_hash = api_hash.strip().lower()
    if not api_id.isdigit() or int(api_id) <= 0:
        raise PatchError("TELEGRAM_API_ID must be a positive integer")
    if not re.fullmatch(r"[0-9a-f]{32}", api_hash):
        raise PatchError("TELEGRAM_API_HASH must be exactly 32 hexadecimal characters")
    if api_id == OFFICIAL_APP_ID or api_hash == OFFICIAL_APP_HASH:
        raise PatchError("Official Telegram API credentials are not permitted for TeleFlow builds")
    return int(api_id), api_hash


def _replace_exact(path: Path, old: str, new: str) -> bool:
    if not path.is_file():
        raise PatchError(f"required upstream file is missing: {path}")
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise PatchError(f"upstream anchor mismatch in {path}: expected exactly one occurrence of {old!r}, found {count}")
    path.write_text(text.replace(old, new), encoding="utf-8")
    return True


def _write_new(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        raise PatchError(f"TeleFlow extension file already exists: {path}")
    path.write_text(content, encoding="utf-8")


def apply_teleflow(root: Path, api_id: str, api_hash: str) -> PatchReport:
    app_id, app_hash = validate_credentials(api_id, api_hash)
    root = Path(root)
    changed: set[Path] = set()

    buildvars = root / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"
    for old, new in (
        ("public static int APP_ID = 4;", f"public static int APP_ID = {app_id};"),
        (
            'public static String APP_HASH = "014b35b6184100b085b0d0572f9b5103";',
            f'public static String APP_HASH = "{app_hash}";',
        ),
        ("public static boolean IS_BILLING_UNAVAILABLE = false;", "public static boolean IS_BILLING_UNAVAILABLE = true;"),
        ("public static boolean SUPPORTS_PASSKEYS = true;", "public static boolean SUPPORTS_PASSKEYS = false;"),
    ):
        _replace_exact(buildvars, old, new)
        changed.add(buildvars)

    gradle_properties = root / "gradle.properties"
    _replace_exact(gradle_properties, "APP_PACKAGE=org.telegram.messenger", "APP_PACKAGE=com.loftfull.teleflow")
    changed.add(gradle_properties)

    strings = root / "TMessagesProj/src/main/res/values/strings.xml"
    _replace_exact(strings, '<string name="AppName">Telegram</string>', '<string name="AppName">TeleFlow</string>')
    _replace_exact(strings, '<string name="AppNameBeta">Telegram Beta</string>', '<string name="AppNameBeta">TeleFlow Beta</string>')
    changed.add(strings)

    base = root / "TMessagesProj/src/main/java/org/telegram/teleflow"
    generated = {
        base / "core/TeleFlowFeatures.java": r'''/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.core;

public final class TeleFlowFeatures {
    public static final String PRODUCT_NAME = "TeleFlow";
    public static final boolean SMART_FOLDERS_BOOTSTRAP = true;
    public static final boolean DRIVE_BOOTSTRAP = true;
    public static final boolean AUTOMATION_BOOTSTRAP = false;
    public static final boolean AI_BOOTSTRAP = false;

    private TeleFlowFeatures() {}
}
''',
        base / "folders/ChatDescriptor.java": r'''/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

public final class ChatDescriptor {
    public enum PeerType { USER, GROUP, CHANNEL, BOT, UNKNOWN }

    private final long dialogId;
    private final String title;
    private final String username;
    private final PeerType peerType;

    public ChatDescriptor(long dialogId, String title, String username, PeerType peerType) {
        this.dialogId = dialogId;
        this.title = title == null ? "" : title;
        this.username = username == null ? "" : username;
        this.peerType = peerType == null ? PeerType.UNKNOWN : peerType;
    }

    public long getDialogId() { return dialogId; }
    public String getTitle() { return title; }
    public String getUsername() { return username; }
    public PeerType getPeerType() { return peerType; }
}
''',
        base / "folders/FolderRule.java": r'''/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class FolderRule {
    private final String folder;
    private final ChatDescriptor.PeerType peerType;
    private final List<String> keywords;

    public FolderRule(String folder, ChatDescriptor.PeerType peerType, List<String> keywords) {
        if (folder == null || folder.trim().isEmpty()) {
            throw new IllegalArgumentException("folder must not be blank");
        }
        this.folder = folder.trim();
        this.peerType = peerType;
        List<String> normalized = new ArrayList<>();
        if (keywords != null) {
            for (String keyword : keywords) {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    normalized.add(keyword.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.keywords = Collections.unmodifiableList(normalized);
    }

    public String getFolder() { return folder; }

    public boolean matches(ChatDescriptor chat) {
        if (chat == null) { return false; }
        if (peerType != null && peerType != chat.getPeerType()) { return false; }
        if (keywords.isEmpty()) { return true; }
        String haystack = (chat.getTitle() + " " + chat.getUsername()).toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (haystack.contains(keyword)) { return true; }
        }
        return false;
    }
}
''',
        base / "folders/ClassificationDecision.java": r'''/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

public final class ClassificationDecision {
    private final String folder;
    private final double confidence;
    private final String reason;

    public ClassificationDecision(String folder, double confidence, String reason) {
        this.folder = folder;
        this.confidence = confidence;
        this.reason = reason == null ? "" : reason;
    }

    public String getFolder() { return folder; }
    public double getConfidence() { return confidence; }
    public String getReason() { return reason; }
}
''',
        base / "folders/FolderRuleEngine.java": r'''/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FolderRuleEngine {
    private final List<FolderRule> rules;
    private final String fallbackFolder;

    public FolderRuleEngine(List<FolderRule> rules, String fallbackFolder) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules == null ? Collections.emptyList() : rules));
        this.fallbackFolder = fallbackFolder == null || fallbackFolder.trim().isEmpty() ? "Review" : fallbackFolder.trim();
    }

    public ClassificationDecision classify(ChatDescriptor chat) {
        for (FolderRule rule : rules) {
            if (rule.matches(chat)) {
                return new ClassificationDecision(rule.getFolder(), 1.0d, "deterministic-rule");
            }
        }
        return new ClassificationDecision(fallbackFolder, 0.0d, "fallback");
    }
}
''',
        base / "drive/VirtualPath.java": r'''/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.drive;

import java.util.ArrayList;
import java.util.List;

public final class VirtualPath {
    private VirtualPath() {}

    public static String normalize(String raw) {
        String value = raw == null ? "" : raw.trim().replace('\\', '/');
        String[] parts = value.split("/+");
        List<String> clean = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) { continue; }
            if ("..".equals(part)) {
                throw new IllegalArgumentException("parent traversal is not allowed in TeleFlow Drive paths");
            }
            clean.add(part);
        }
        return clean.isEmpty() ? "/" : "/" + String.join("/", clean);
    }
}
''',
        base / "drive/DriveObject.java": r'''/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.drive;

public final class DriveObject {
    private final int messageId;
    private final long peerId;
    private final String name;
    private final String mimeType;
    private final long size;
    private final String virtualPath;

    public DriveObject(int messageId, long peerId, String name, String mimeType, long size, String virtualPath) {
        if (size < 0) { throw new IllegalArgumentException("size must be non-negative"); }
        this.messageId = messageId;
        this.peerId = peerId;
        this.name = name == null ? "" : name;
        this.mimeType = mimeType == null ? "application/octet-stream" : mimeType;
        this.size = size;
        this.virtualPath = VirtualPath.normalize(virtualPath);
    }

    public int getMessageId() { return messageId; }
    public long getPeerId() { return peerId; }
    public String getName() { return name; }
    public String getMimeType() { return mimeType; }
    public long getSize() { return size; }
    public String getVirtualPath() { return virtualPath; }
}
''',
    }
    for path, content in generated.items():
        _write_new(path, content)
        changed.add(path)

    return PatchReport(changed_files=len(changed))


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply deterministic TeleFlow changes to an official Telegram Android checkout")
    parser.add_argument("root", type=Path)
    parser.add_argument("--api-id", required=True)
    parser.add_argument("--api-hash", required=True)
    args = parser.parse_args()
    try:
        report = apply_teleflow(args.root, args.api_id, args.api_hash)
    except PatchError as exc:
        parser.error(str(exc))
    print(f"TeleFlow patch applied: {report.changed_files} files changed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
