/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.telegram.messenger.MessagesController;
import org.telegram.teleflow.folders.FolderAssignment;
import org.telegram.teleflow.folders.FolderPreviewPlan;
import org.telegram.teleflow.folders.FolderProvisioningPlan;
import org.telegram.teleflow.folders.FolderProvisioningPlanner;
import org.telegram.teleflow.folders.ManagedFolderCatalog;
import org.telegram.teleflow.folders.ManagedFolderResolution;
import org.telegram.teleflow.folders.ManagedFolderSpec;
import org.telegram.teleflow.folders.SmartFoldersBatchCoordinator;
import org.telegram.ui.ActionBar.BaseFragment;

public final class SmartFoldersBatchApply {
    public interface Completion {
        void onComplete(SmartFoldersBatchCoordinator.Result result);
    }

    private SmartFoldersBatchApply() {}

    public static void apply(BaseFragment fragment, int account, FolderPreviewPlan preview, String reviewFolderName, Completion completion) {
        if (fragment == null || fragment.getParentActivity() == null || preview == null || completion == null) {
            throw new IllegalArgumentException("attached fragment, preview and completion are required");
        }
        Context context = fragment.getParentActivity();
        ManagedFolderInventory inventory = ManagedFolderResolver.resolve(context, account);
        Map<String, List<Long>> desiredByKey = desiredByKey(preview, reviewFolderName);

        for (Map.Entry<String, ManagedFolderResolution.Status> entry : inventory.getStatuses().entrySet()) {
            if (entry.getValue() == ManagedFolderResolution.Status.IDENTITY_MISMATCH
                || entry.getValue() == ManagedFolderResolution.Status.MISSING) {
                completion.onComplete(failure("managed-folder-conflict", entry.getKey()));
                return;
            }
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ManagedFolderSpec spec : ManagedFolderCatalog.create()) {
            counts.put(spec.getDisplayName(), desiredByKey.get(spec.getLogicalKey()).size());
        }
        FolderProvisioningPlan provisioning = FolderProvisioningPlanner.plan(
            counts,
            inventory.getActiveLogicalKeys(),
            inventory.getCurrentUserFolderCount(),
            inventory.getFolderLimit()
        );
        List<ManagedFolderSpec> blockedByLimit = provisioning.getBlockedByLimit();
        if (!blockedByLimit.isEmpty()) {
            completion.onComplete(failure("folder-limit", blockedByLimit.get(0).getLogicalKey()));
            return;
        }

        List<SmartFoldersBatchCoordinator.Operation> operations = new ArrayList<>();
        MessagesController controller = fragment.getMessagesController();
        ManagedFolderStateStore managedStateStore = new ManagedFolderStateStore(context, account);
        for (ManagedFolderSpec spec : ManagedFolderCatalog.create()) {
            List<Long> desired = desiredByKey.get(spec.getLogicalKey());
            Integer folderId = inventory.getActiveFolderId(spec.getLogicalKey());
            if (folderId != null) {
                MessagesController.DialogFilter filter = controller.dialogFiltersById.get(folderId);
                if (filter == null) {
                    completion.onComplete(failure("managed-folder-missing-live-object", spec.getLogicalKey()));
                    return;
                }
                if (!desired.isEmpty() || managedStateStore.load(folderId) != null) {
                    operations.add(existingOperation(fragment, account, spec, filter, desired));
                }
            } else if (!desired.isEmpty()) {
                operations.add(newOperation(fragment, account, spec, desired));
            }
        }
        SmartFoldersBatchCoordinator.apply(operations, completion::onComplete);
    }

    private static Map<String, List<Long>> desiredByKey(FolderPreviewPlan preview, String reviewFolderName) {
        Map<String, List<Long>> result = new LinkedHashMap<>();
        for (ManagedFolderSpec spec : ManagedFolderCatalog.create()) {
            result.put(spec.getLogicalKey(), new ArrayList<>());
        }
        for (Map.Entry<String, List<FolderAssignment>> entry : preview.getFolders().entrySet()) {
            ManagedFolderSpec spec = entry.getKey().equals(reviewFolderName)
                ? ManagedFolderCatalog.byLogicalKey("review")
                : ManagedFolderCatalog.byDisplayName(entry.getKey());
            if (spec == null) continue;
            List<Long> ids = result.get(spec.getLogicalKey());
            for (FolderAssignment assignment : entry.getValue()) {
                ids.add(assignment.getChat().getDialogId());
            }
        }
        return result;
    }

    private static SmartFoldersBatchCoordinator.Operation existingOperation(
        BaseFragment fragment,
        int account,
        ManagedFolderSpec spec,
        MessagesController.DialogFilter filter,
        List<Long> desired
    ) {
        SmartFoldersTransaction transaction = new SmartFoldersTransaction(fragment, filter, account);
        return new SmartFoldersBatchCoordinator.Operation() {
            @Override public String key() { return spec.getLogicalKey(); }
            @Override public void apply(SmartFoldersBatchCoordinator.StepCallback callback) {
                transaction.apply(desired, result -> callback.onResult(result.isSuccess(), result.getCode() + ":" + result.getDetail()));
            }
            @Override public void rollback(SmartFoldersBatchCoordinator.StepCallback callback) {
                transaction.undo(result -> callback.onResult(result.isSuccess(), result.getCode() + ":" + result.getDetail()));
            }
        };
    }

    private static SmartFoldersBatchCoordinator.Operation newOperation(
        BaseFragment fragment,
        int account,
        ManagedFolderSpec spec,
        List<Long> desired
    ) {
        final MessagesController.DialogFilter[] created = new MessagesController.DialogFilter[1];
        return new SmartFoldersBatchCoordinator.Operation() {
            @Override public String key() { return spec.getLogicalKey(); }
            @Override public void apply(SmartFoldersBatchCoordinator.StepCallback callback) {
                ManagedFolderProvisioner.provision(fragment, account, spec, desired, (success, filter, code, detail) -> {
                    created[0] = filter;
                    callback.onResult(success, code + ":" + (detail == null ? "" : detail));
                });
            }
            @Override public void rollback(SmartFoldersBatchCoordinator.StepCallback callback) {
                if (created[0] == null) {
                    callback.onResult(true, null);
                    return;
                }
                TelegramFolderGateway.deleteFolder(fragment, created[0], (success, error) -> {
                    if (!success) {
                        callback.onResult(false, error);
                        return;
                    }
                    Context context = fragment.getParentActivity();
                    boolean registryCleared = new ManagedFolderRegistryStore(context, account).delete(spec.getLogicalKey());
                    boolean managedCleared = new ManagedFolderStateStore(context, account).delete(created[0].id);
                    callback.onResult(
                        registryCleared && managedCleared,
                        registryCleared && managedCleared ? null : "local-cleanup-failed"
                    );
                });
            }
        };
    }

    private static SmartFoldersBatchCoordinator.Result failure(String code, String detail) {
        return SmartFoldersBatchCoordinator.Result.fail(code, detail, detail);
    }
}
