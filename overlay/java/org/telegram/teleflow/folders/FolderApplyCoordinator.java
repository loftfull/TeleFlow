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

    public interface ManagedBackupPersistence {
        boolean save(ManagedFolderState state);
        ManagedFolderState load(int folderId);
        boolean delete(int folderId);
    }

    public interface ManagedPersistence {
        ManagedFolderState load(int folderId);
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
        ManagedBackupPersistence managedBackup,
        ManagedPersistence managedPersistence,
        Remote remote,
        Completion completion
    ) {
        require(current, "current");
        require(managed, "managed");
        require(backup, "backup");
        require(managedBackup, "managedBackup");
        require(managedPersistence, "managedPersistence");
        require(remote, "remote");
        require(completion, "completion");

        List<Long> desired = deduplicate(desiredManagedDialogIds);
        FolderUpdatePlan plan = FolderMutationPlanner.merge(current, managed, desired);

        if (!backup.save(current)) {
            completion.onComplete(Result.fail("backup-save-failed", "Refusing remote mutation without durable server backup"));
            return;
        }
        if (!managedBackup.save(managed)) {
            backup.delete(current.getFolderId());
            completion.onComplete(Result.fail("managed-backup-save-failed", "Refusing remote mutation without durable managed-state backup"));
            return;
        }

        remote.apply(plan, (success, error) -> {
            if (!success) {
                cleanupBackups(current.getFolderId(), backup, managedBackup);
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
                    cleanupBackups(current.getFolderId(), backup, managedBackup);
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
        FolderServerSnapshot current,
        int folderId,
        BackupPersistence backup,
        ManagedBackupPersistence managedBackup,
        ManagedPersistence managedPersistence,
        Remote remote,
        Completion completion
    ) {
        require(current, "current");
        require(backup, "backup");
        require(managedBackup, "managedBackup");
        require(managedPersistence, "managedPersistence");
        require(remote, "remote");
        require(completion, "completion");

        FolderServerSnapshot snapshot = backup.load(folderId);
        ManagedFolderState previousManaged = managedBackup.load(folderId);
        if (snapshot == null || previousManaged == null) {
            completion.onComplete(Result.fail("backup-missing", "Server or managed-state backup is missing"));
            return;
        }

        remote.restore(snapshot, (success, error) -> {
            if (!success) {
                completion.onComplete(Result.fail("remote-undo-failed", error));
                return;
            }
            if (managedPersistence.save(previousManaged)) {
                cleanupBackups(folderId, backup, managedBackup);
                completion.onComplete(Result.ok());
                return;
            }
            remote.restore(current, (rollbackSuccess, rollbackError) -> {
                if (rollbackSuccess) {
                    completion.onComplete(Result.fail(
                        "managed-state-restore-failed-undo-rolled-back",
                        "Undo was rolled back because previous managed state could not be restored"
                    ));
                } else {
                    completion.onComplete(Result.fail(
                        "managed-state-restore-failed-rollback-failed",
                        rollbackError
                    ));
                }
            });
        });
    }

    private static void cleanupBackups(int folderId, BackupPersistence backup, ManagedBackupPersistence managedBackup) {
        backup.delete(folderId);
        managedBackup.delete(folderId);
    }

    private static List<Long> deduplicate(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null) unique.add(value);
        }
        return new ArrayList<>(unique);
    }

    private static void require(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
    }
}
