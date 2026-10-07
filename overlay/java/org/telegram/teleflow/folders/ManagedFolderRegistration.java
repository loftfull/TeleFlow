/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

public final class ManagedFolderRegistration {
    private final String logicalKey;
    private final int folderId;
    private final String expectedServerName;

    public ManagedFolderRegistration(String logicalKey, int folderId, String expectedServerName) {
        if (logicalKey == null || logicalKey.trim().isEmpty()) {
            throw new IllegalArgumentException("logicalKey is required");
        }
        if (folderId < 2) {
            throw new IllegalArgumentException("managed folderId must be >= 2");
        }
        if (expectedServerName == null || expectedServerName.trim().isEmpty()) {
            throw new IllegalArgumentException("expectedServerName is required");
        }
        this.logicalKey = logicalKey.trim();
        this.folderId = folderId;
        this.expectedServerName = expectedServerName.trim();
    }

    public String getLogicalKey() { return logicalKey; }
    public int getFolderId() { return folderId; }
    public String getExpectedServerName() { return expectedServerName; }
}
