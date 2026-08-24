/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class FolderApplyCoordinator {
    private FolderApplyCoordinator() {}

    public interface BackupPersistence {
        boolean save(FolderServerSnapshot snapshot);
        FolderServerSnapshot load(int folderId);
        boolean delete(int folderId);
    }

    public interface ManagedPersistence {
        boolean save(ManagedFolderState state);
        boolean delete(int folderId);
    }

    public interface RemoteCallback {
        void onResult(boolean success, String error);
    }

    public interface Remote {
        void apply(FolderUpdatePlan plan, RemoteCallback callback);
        void restore(FolderServerSnapshot snapshot, RemoteCallback callback);
    }

    public interface Completion {
        void onComplete(Result result);
    }

    public static final class Result {
        private final boolean success;
        private final String code;
        private final String detail;

        private Result(boolean success, String code, String detail) {
            this.success = success;
            this.code = code;
            this.detail = detail == null ? "" : detail;
        }

        public static Result ok() {
            return new Result(true, "ok", "");
        }

        public static Result fail(String code, String detail) {
            return new Result(false, code, detail);
        }

        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getDetail() { return detail; }
    }

    public static void apply(
        FolderServerSnapshot current,
        ManagedFolderState managed,
        List<Long> desiredManagedDialogIds,
        BackupPersistence backup,
        ManagedPersistence managedPersistence,
        Remote remote,
        Completion completion
    ) {
        require(current, "current");
        require(managed, "managed");
        require(backup, "backup");
        require(managedPersistence, "managedPersistence");
        require(remote, "remote");
        require(completion, "completion");

        List<Long> desired = deduplicate(desiredManagedDialogIds);
        FolderUpdatePlan plan = FolderMutationPlanner.merge(current, managed, desired);

        if (!backup.save(current)) {
            completion.onComplete(Result.fail("backup-save-failed", "Refusing remote mutation without durable backup"));
            return;
        }

        remote.apply(plan, (success, error) -> {
            if (!success) {
                completion.onComplete(Result.fail("remote-apply-failed", error));
                return;
            }

            ManagedFolderState nextState = new ManagedFolderState(plan.getFolderId(), plan.getFolderName(), desired);
            if (managedPersistence.save(nextState)) {
                completion.onComplete(Result.ok());
                return;
            }

            remote.restore(current, (rollbackSuccess, rollbackError) -> {
                if (rollbackSuccess) {
                    completion.onComplete(Result.fail(
                        "managed-state-save-failed-rolled-back",
                        "Remote mutation was rolled back because local managed state could not be persisted"
                    ));
                } else {
                    completion.onComplete(Result.fail(
                        "managed-state-save-failed-rollback-failed",
                        rollbackError
                    ));
                }
            });
        });
    }

    public static void undo(
        int folderId,
        BackupPersistence backup,
        ManagedPersistence managedPersistence,
        Remote remote,
        Completion completion
    ) {
        require(backup, "backup");
        require(managedPersistence, "managedPersistence");
        require(remote, "remote");
        require(completion, "completion");

        FolderServerSnapshot snapshot = backup.load(folderId);
        if (snapshot == null) {
            completion.onComplete(Result.fail("backup-missing", "No backup exists for folder"));
            return;
        }

        remote.restore(snapshot, (success, error) -> {
            if (!success) {
                completion.onComplete(Result.fail("remote-undo-failed", error));
                return;
            }
            if (!managedPersistence.delete(folderId)) {
                completion.onComplete(Result.fail(
                    "managed-state-delete-failed",
                    "Folder was restored remotely, but local managed state could not be cleared"
                ));
                return;
            }
            backup.delete(folderId);
            completion.onComplete(Result.ok());
        });
    }

    private static List<Long> deduplicate(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null) {
                unique.add(value);
            }
        }
        return new ArrayList<>(unique);
    }

    private static void require(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
