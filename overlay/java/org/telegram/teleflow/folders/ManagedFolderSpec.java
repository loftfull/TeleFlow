/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

public final class ManagedFolderSpec {
    private final String logicalKey;
    private final String displayName;
    private final String serverName;

    public ManagedFolderSpec(String logicalKey, String displayName, String serverName) {
        if (logicalKey == null || logicalKey.trim().isEmpty()) {
            throw new IllegalArgumentException("logicalKey is required");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("displayName is required");
        }
        if (serverName == null || serverName.trim().isEmpty()) {
            throw new IllegalArgumentException("serverName is required");
        }
        String normalizedServerName = serverName.trim();
        if (normalizedServerName.codePointCount(0, normalizedServerName.length()) > 12) {
            throw new IllegalArgumentException("Telegram folder names must be at most 12 code points");
        }
        this.logicalKey = logicalKey.trim();
        this.displayName = displayName.trim();
        this.serverName = normalizedServerName;
    }

    public String getLogicalKey() { return logicalKey; }
    public String getDisplayName() { return displayName; }
    public String getServerName() { return serverName; }
}
