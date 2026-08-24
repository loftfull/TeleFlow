/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import java.util.Collections;
import java.util.List;

import org.telegram.messenger.MessagesController;
import org.telegram.teleflow.folders.FolderApplyCoordinator;
import org.telegram.teleflow.folders.FolderServerSnapshot;
import org.telegram.teleflow.folders.FolderUpdatePlan;
import org.telegram.teleflow.folders.ManagedFolderState;
import org.telegram.ui.ActionBar.BaseFragment;

public final class SmartFoldersTransaction {
    private final BaseFragment fragment;
    private final MessagesController.DialogFilter filter;
    private final FolderBackupStore backupStore;
    private final ManagedFolderStateStore managedStore;

    public SmartFoldersTransaction(BaseFragment fragment, MessagesController.DialogFilter filter, int account) {
        if (fragment == null || fragment.getParentActivity() == null) {
            throw new IllegalArgumentException("attached fragment is required");
        }
        if (filter == null) {
            throw new IllegalArgumentException("filter is required");
        }
        this.fragment = fragment;
        this.filter = filter;
        this.backupStore = new FolderBackupStore(fragment.getParentActivity(), account);
        this.managedStore = new ManagedFolderStateStore(fragment.getParentActivity(), account);
    }

    public void apply(List<Long> desiredManagedDialogIds, FolderApplyCoordinator.Completion completion) {
        FolderServerSnapshot current = TelegramFolderGateway.snapshot(filter);
        ManagedFolderState managed = managedStore.load(filter.id);
        if (managed == null) {
            managed = new ManagedFolderState(filter.id, filter.name, Collections.emptyList());
        }
        FolderApplyCoordinator.apply(
            current,
            managed,
            desiredManagedDialogIds,
            backupPersistence(),
            managedPersistence(),
            remote(),
            completion
        );
    }

    public void undo(FolderApplyCoordinator.Completion completion) {
        FolderApplyCoordinator.undo(
            filter.id,
            backupPersistence(),
            managedPersistence(),
            remote(),
            completion
        );
    }

    private FolderApplyCoordinator.BackupPersistence backupPersistence() {
        return new FolderApplyCoordinator.BackupPersistence() {
            @Override
            public boolean save(FolderServerSnapshot snapshot) { return backupStore.save(snapshot); }
            @Override
            public FolderServerSnapshot load(int folderId) { return backupStore.load(folderId); }
            @Override
            public boolean delete(int folderId) { return backupStore.delete(folderId); }
        };
    }

    private FolderApplyCoordinator.ManagedPersistence managedPersistence() {
        return new FolderApplyCoordinator.ManagedPersistence() {
            @Override
            public boolean save(ManagedFolderState state) { return managedStore.save(state); }
            @Override
            public boolean delete(int folderId) { return managedStore.delete(folderId); }
        };
    }

    private FolderApplyCoordinator.Remote remote() {
        return new FolderApplyCoordinator.Remote() {
            @Override
            public void apply(FolderUpdatePlan plan, FolderApplyCoordinator.RemoteCallback callback) {
                TelegramFolderGateway.applyExisting(fragment, filter, plan, callback::onResult);
            }

            @Override
            public void restore(FolderServerSnapshot snapshot, FolderApplyCoordinator.RemoteCallback callback) {
                TelegramFolderGateway.restoreSnapshot(fragment, filter, snapshot, callback::onResult);
            }
        };
    }
}
