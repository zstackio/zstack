package org.zstack.kvm.memory;

import java.util.Arrays;

/** Reject action-inapplicable parameters before staging, hashing or persistence. */
final class MemoryPolicyActionParameters {
    private MemoryPolicyActionParameters() { }

    static void validate(APIUpdateMemoryPolicyMsg msg) {
        String action = msg.getAction();
        if (!Arrays.asList("apply", "clearOverride", "pause", "drain", "resume", "reconcile",
                "recoverUncertain", "prepareWritebackBackend", "prepareZramPool", "stageTargetShard",
                "commitTargetShards", "cancelTargetShards").contains(action)) {
            invalid("action");
        }
        boolean stage = "stageTargetShard".equals(action);
        boolean commit = "commitTargetShards".equals(action);
        boolean cancel = "cancelTargetShards".equals(action);
        if (!stage && !commit && (msg.getTargetSnapshotGeneration() != null || msg.getTargetShardIndex() != null
                || msg.getTargetShardCount() != null || msg.getTargetTotalCount() != null
                || msg.getTargetDigest() != null || msg.getTargetVmUuids() != null)) {
            invalid("target shard parameters");
        }
        if (commit && (msg.getTargetShardIndex() != null || msg.getTargetVmUuids() != null)) {
            invalid("targetShardIndex/targetVmUuids (commit uses staged targets)");
        }
        if (!"clearOverride".equals(action) && msg.getClearOverrideFields() != null) {
            invalid("clearOverrideFields");
        }
        if (!"VM".equals(msg.getScope()) && msg.getExpectedInstanceGeneration() != null) {
            invalid("expectedInstanceGeneration");
        }
        if (stage || commit || cancel) {
            if ("VM".equals(msg.getScope())) { invalid("VM target-shard scope"); }
            if (msg.getExpectedControlOperationUuid() != null || msg.getExpectedInstanceGeneration() != null) {
                invalid("operation/instance fences on target staging");
            }
        }
        if (stage || cancel) {
            if (!MemoryTaskRules.isSafetyAction("pause", msg.getPolicy())) { invalid("policy"); }
            if (msg.getExpectedSourceRevisions() != null || msg.getExpectedGlobalRevision() != null) {
                invalid("source revisions on target staging");
            }
        }
        if (stage && msg.getTargetVmUuids() != null && msg.getTargetHostUuids() != null) {
            invalid("ambiguous targetVmUuids/targetHostUuids");
        }
        if (cancel && msg.getTargetHostUuids() != null) { invalid("targetHostUuids on staging cancellation"); }
    }

    private static void invalid(String field) {
        throw new MemoryOperationException("MEMORY_INVALID_REQUEST", "Parameter is not applicable to this action: " + field);
    }
}
