/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SmartFoldersBatchCoordinator {
    public interface StepCallback {
        void onResult(boolean success, String error);
    }

    public interface Operation {
        String key();
        void apply(StepCallback callback);
        void rollback(StepCallback callback);
    }

    public interface Completion {
        void onComplete(Result result);
    }

    public static final class Result {
        private final boolean success;
        private final String code;
        private final String detail;
        private final String failedKey;
        private final List<String> rollbackFailures;

        private Result(boolean success, String code, String detail, String failedKey, List<String> rollbackFailures) {
            this.success = success;
            this.code = code;
            this.detail = detail == null ? "" : detail;
            this.failedKey = failedKey;
            this.rollbackFailures = Collections.unmodifiableList(new ArrayList<>(rollbackFailures));
        }

        public static Result fail(String code, String detail, String failedKey) {
            return new Result(false, code, detail, failedKey, Collections.emptyList());
        }

        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getDetail() { return detail; }
        public String getFailedKey() { return failedKey; }
        public List<String> getRollbackFailures() { return rollbackFailures; }
    }

    private SmartFoldersBatchCoordinator() {}

    public static void apply(List<Operation> operations, Completion completion) {
        if (completion == null) throw new IllegalArgumentException("completion is required");
        List<Operation> ops = operations == null ? Collections.emptyList() : new ArrayList<>(operations);
        applyNext(ops, 0, new ArrayList<>(), completion);
    }

    private static void applyNext(List<Operation> ops, int index, List<Operation> completed, Completion completion) {
        if (index >= ops.size()) {
            completion.onComplete(new Result(true, "ok", "", null, Collections.emptyList()));
            return;
        }
        Operation operation = ops.get(index);
        if (operation == null) {
            rollback(completed, completed.size() - 1, "null-operation", "Operation is null", null, new ArrayList<>(), completion);
            return;
        }
        operation.apply((success, error) -> {
            if (!success) {
                rollback(completed, completed.size() - 1, "apply-failed", error, safeKey(operation), new ArrayList<>(), completion);
                return;
            }
            completed.add(operation);
            applyNext(ops, index + 1, completed, completion);
        });
    }

    private static void rollback(
        List<Operation> completed,
        int index,
        String originalCode,
        String originalDetail,
        String failedKey,
        List<String> rollbackFailures,
        Completion completion
    ) {
        if (index < 0) {
            String code = rollbackFailures.isEmpty() ? "apply-failed-rolled-back" : "apply-failed-rollback-failed";
            completion.onComplete(new Result(false, code, originalDetail == null ? originalCode : originalDetail, failedKey, rollbackFailures));
            return;
        }
        Operation operation = completed.get(index);
        operation.rollback((success, error) -> {
            if (!success) {
                rollbackFailures.add(safeKey(operation) + ":" + (error == null ? "rollback-failed" : error));
            }
            rollback(completed, index - 1, originalCode, originalDetail, failedKey, rollbackFailures, completion);
        });
    }

    private static String safeKey(Operation operation) {
        if (operation == null || operation.key() == null || operation.key().trim().isEmpty()) return "unknown";
        return operation.key();
    }
}
