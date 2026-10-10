package org.zstack.kvm.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admission budget for one management-node memory API request.
 *
 * This deliberately covers the request envelope and one policy/shard only.
 * A target list assembled from several admitted shards is checked by the
 * shard protocol and is not re-rejected here as though it were one request.
 */
public final class MemoryApiRequestBudget {
    public static final int DEFAULT_BYTES = 1 << 20;
    private static final Gson JSON = new GsonBuilder().disableHtmlEscaping().create();

    private MemoryApiRequestBudget() { }

    public static void validatePolicy(String policy, long budgetBytes) {
        validateBytes(utf8(policy), budgetBytes);
    }

    public static void validateUpdate(APIUpdateMemoryPolicyMsg message) {
        validateUpdate(message, configuredBudget());
    }

    public static void validateUpdate(APIUpdateMemoryPolicyMsg message, long budgetBytes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "scope", message.getScope()); put(payload, "resourceUuid", message.getResourceUuid());
        put(payload, "action", message.getAction()); put(payload, "policy", message.getPolicy());
        put(payload, "backendPreparation", message.getBackendPreparation());
        put(payload, "poolPreparation", message.getPoolPreparation());
        put(payload, "recovery", message.getRecovery());
        put(payload, "clientRequestUuid", message.getClientRequestUuid());
        put(payload, "expectedInstanceGeneration", message.getExpectedInstanceGeneration());
        put(payload, "expectedControlOperationUuid", message.getExpectedControlOperationUuid());
        put(payload, "targetSnapshotGeneration", message.getTargetSnapshotGeneration());
        put(payload, "targetDigest", message.getTargetDigest());
        put(payload, "targetHostUuids", message.getTargetHostUuids()); put(payload, "targetVmUuids", message.getTargetVmUuids());
        put(payload, "clearOverrideFields", message.getClearOverrideFields());
        put(payload, "expectedSourceRevisions", message.getExpectedSourceRevisions());
        put(payload, "expectedGlobalRevision", message.getExpectedGlobalRevision());
        put(payload, "expectedRevision", message.getExpectedRevision());
        put(payload, "targetShardIndex", message.getTargetShardIndex()); put(payload, "targetShardCount", message.getTargetShardCount());
        put(payload, "targetTotalCount", message.getTargetTotalCount());
        validateBytes(utf8(JSON.toJson(payload)), budgetBytes);
    }

    public static void validatePreview(APIPreviewMemoryPolicyMsg message) {
        validatePreview(message, configuredBudget());
    }

    public static void validatePreview(APIPreviewMemoryPolicyMsg message, long budgetBytes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "scope", message.getScope()); put(payload, "resourceUuid", message.getResourceUuid());
        put(payload, "policy", message.getPolicy()); put(payload, "targetHostUuids", message.getTargetHostUuids());
        put(payload, "action", message.getAction());
        put(payload, "clearOverrideFields", message.getClearOverrideFields());
        validateBytes(utf8(JSON.toJson(payload)), budgetBytes);
    }

    static int configuredBudget() {
        return MemoryOptimizationGlobalConfig.positive(MemoryOptimizationGlobalConfig.REQUEST_BYTES, DEFAULT_BYTES);
    }

    static long utf8(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static void put(Map<String, Object> payload, String key, Object value) {
        if (value != null) { payload.put(key, value); }
    }

    private static void validateBytes(long actual, long budgetBytes) {
        if (budgetBytes <= 0 || actual > budgetBytes) {
            throw new MemoryOperationException("MEMORY_REQUEST_TOO_LARGE",
                    "Memory API request is " + actual + " UTF-8 bytes; budget is " + budgetBytes);
        }
    }
}
