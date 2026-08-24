/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FolderServerSnapshot {
    private final int folderId;
    private final String folderName;
    private final List<Long> includeDialogIds;
    private final List<Long> pinnedDialogIds;
    private final List<Long> excludeDialogIds;

    public FolderServerSnapshot(int folderId, String folderName, List<Long> includeDialogIds, List<Long> pinnedDialogIds, List<Long> excludeDialogIds) {
        this.folderId = folderId;
        this.folderName = folderName == null ? "" : folderName;
        this.includeDialogIds = immutable(includeDialogIds);
        this.pinnedDialogIds = immutable(pinnedDialogIds);
        this.excludeDialogIds = immutable(excludeDialogIds);
    }

    private static List<Long> immutable(List<Long> values) {
        return Collections.unmodifiableList(new ArrayList<>(values == null ? Collections.emptyList() : values));
    }

    public int getFolderId() { return folderId; }
    public String getFolderName() { return folderName; }
    public List<Long> getIncludeDialogIds() { return includeDialogIds; }
    public List<Long> getPinnedDialogIds() { return pinnedDialogIds; }
    public List<Long> getExcludeDialogIds() { return excludeDialogIds; }
}
