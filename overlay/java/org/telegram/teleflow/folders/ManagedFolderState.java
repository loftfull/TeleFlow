/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ManagedFolderState {
    private final int folderId;
    private final String folderName;
    private final List<Long> managedDialogIds;

    public ManagedFolderState(int folderId, String folderName, List<Long> managedDialogIds) {
        if (folderId < 0) { throw new IllegalArgumentException("folderId must be non-negative"); }
        this.folderId = folderId;
        this.folderName = folderName == null ? "" : folderName;
        this.managedDialogIds = Collections.unmodifiableList(new ArrayList<>(managedDialogIds == null ? Collections.emptyList() : managedDialogIds));
    }

    public int getFolderId() { return folderId; }
    public String getFolderName() { return folderName; }
    public List<Long> getManagedDialogIds() { return managedDialogIds; }
}
