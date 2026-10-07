/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FolderProvisioningPlan {
    private final int availableSlotsBefore;
    private final List<ManagedFolderSpec> create;
    private final List<ManagedFolderSpec> blockedByLimit;

    public FolderProvisioningPlan(int availableSlotsBefore, List<ManagedFolderSpec> create, List<ManagedFolderSpec> blockedByLimit) {
        this.availableSlotsBefore = Math.max(0, availableSlotsBefore);
        this.create = Collections.unmodifiableList(new ArrayList<>(create == null ? Collections.emptyList() : create));
        this.blockedByLimit = Collections.unmodifiableList(new ArrayList<>(blockedByLimit == null ? Collections.emptyList() : blockedByLimit));
    }

    public int getAvailableSlotsBefore() { return availableSlotsBefore; }
    public List<ManagedFolderSpec> getCreate() { return create; }
    public List<ManagedFolderSpec> getBlockedByLimit() { return blockedByLimit; }
}
