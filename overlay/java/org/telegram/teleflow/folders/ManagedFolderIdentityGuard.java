/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.Collections;
import java.util.Map;

public final class ManagedFolderIdentityGuard {
    private ManagedFolderIdentityGuard() {}

    public static ManagedFolderResolution resolve(ManagedFolderRegistration registration, Map<Integer, String> liveFolders) {
        if (registration == null) {
            return new ManagedFolderResolution(ManagedFolderResolution.Status.UNREGISTERED, null, null);
        }
        Map<Integer, String> live = liveFolders == null ? Collections.emptyMap() : liveFolders;
        if (!live.containsKey(registration.getFolderId())) {
            return new ManagedFolderResolution(ManagedFolderResolution.Status.MISSING, registration, null);
        }
        String liveName = live.get(registration.getFolderId());
        if (!registration.getExpectedServerName().equals(liveName)) {
            return new ManagedFolderResolution(ManagedFolderResolution.Status.IDENTITY_MISMATCH, registration, liveName);
        }
        return new ManagedFolderResolution(ManagedFolderResolution.Status.ACTIVE, registration, liveName);
    }
}
