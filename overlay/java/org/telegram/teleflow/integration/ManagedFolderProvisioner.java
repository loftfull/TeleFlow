/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import android.content.Context;

import java.util.List;

import org.telegram.messenger.MessagesController;
import org.telegram.teleflow.folders.ManagedFolderRegistration;
import org.telegram.teleflow.folders.ManagedFolderSpec;
import org.telegram.teleflow.folders.ManagedFolderState;
import org.telegram.ui.ActionBar.BaseFragment;

public final class ManagedFolderProvisioner {
    public interface Completion {
        void onComplete(boolean success, MessagesController.DialogFilter filter, String code, String detail);
    }

    private ManagedFolderProvisioner() {}

    public static void provision(BaseFragment fragment, int account, ManagedFolderSpec spec, List<Long> desiredDialogIds, Completion completion) {
        if (fragment == null || fragment.getParentActivity() == null || spec == null || completion == null) {
            throw new IllegalArgumentException("attached fragment, spec and completion are required");
        }
        Context context = fragment.getParentActivity();
        ManagedFolderRegistryStore registry = new ManagedFolderRegistryStore(context, account);
        ManagedFolderStateStore managed = new ManagedFolderStateStore(context, account);

        TelegramFolderGateway.createNew(fragment, spec, desiredDialogIds, (success, filter, error) -> {
            if (!success || filter == null) {
                completion.onComplete(false, null, "remote-create-failed", error);
                return;
            }
            ManagedFolderRegistration registration = new ManagedFolderRegistration(spec.getLogicalKey(), filter.id, spec.getServerName());
            if (!registry.saveRegistration(registration)) {
                TelegramFolderGateway.deleteFolder(fragment, filter, (rolledBack, rollbackError) -> completion.onComplete(
                    false,
                    null,
                    rolledBack ? "registry-save-failed-rolled-back" : "registry-save-failed-rollback-failed",
                    rollbackError
                ));
                return;
            }
            ManagedFolderState state = new ManagedFolderState(filter.id, filter.name, desiredDialogIds);
            if (!managed.save(state)) {
                TelegramFolderGateway.deleteFolder(fragment, filter, (rolledBack, rollbackError) -> {
                    if (rolledBack) {
                        registry.delete(spec.getLogicalKey());
                        completion.onComplete(false, null, "managed-state-save-failed-rolled-back", rollbackError);
                    } else {
                        // Keep ownership metadata because the remote folder still exists and must remain recoverable.
                        completion.onComplete(false, filter, "managed-state-save-failed-rollback-failed", rollbackError);
                    }
                });
                return;
            }
            completion.onComplete(true, filter, "ok", null);
        });
    }
}
