package org.zstack.kvm.memory;

import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;

/** Strict merge-patch boundary; typed configuration is serialized only after validation. */
public final class MemoryPolicyRules {
    private static final Gson GSON = new Gson();
    private static final long UINT_MAX = 0xffffffffL;
    private static final Set<String> KSM = keys("enabled,zeroPagesEnabled,pagesToScan,sleepMillis");
    private static final Set<String> ZRAM = keys("enabled,logicalCapacityBytes,ramLimitBytes,requiredAvailableBytes,hostFloorBytes,metadataReserveBytes,transientReserveBytes,cpuThresholdPercent,cpuHoldSeconds,operationIntervalSeconds,"
            + "selectionMode,selectedVmUuids,discoveryIntervalSeconds,discoveryTimeoutSeconds,discoveryTtlSeconds,"
            + "hostSampleIntervalSeconds,hostSampleTtlSeconds,vmSampleIntervalSeconds,vmSampleTtlSeconds,"
            + "hostCpuGuardEnabled,hostMemoryPsiGuardEnabled,hostIoPsiGuardEnabled,hostCpuThresholdPercent,hostMemoryPsiThresholdPercent,hostIoPsiThresholdPercent,"
            + "reclaimBatchBytes,concurrency,timeoutIsolationSlots,reclaimSlowOperationSeconds,operationRecordBudgetBytes,startupObservationSeconds,algorithm");
    private static final Set<String> WRITEBACK = keys("enabled,backendResourceUuid,backendCapacityBytes,batchBytes,idleObservationSeconds,operationIntervalSeconds,"
            + "slowOperationSeconds,noProgressLimit,backoffSeconds,ioErrorBackoffSeconds,metadataBudgetBytes,filesystemReserveBytes");

    private MemoryPolicyRules() { }

    public static String defaults() {
        // Upgrade bootstrap is deliberately inert. Fresh-install target policy is
        // applied separately after capability checks, never inferred from missing data.
        return "{\"schemaVersion\":1,\"ksm\":{},\"zram\":{\"enabled\":false},\"writeback\":{\"enabled\":false}}";
    }

    private static Set<String> keys(String names) {
        return new HashSet<>(Arrays.asList(names.split(",")));
    }

    public static String merge(String current, String patch, String scope) {
        String merged = mergeOverridesOnly(current, patch, scope);
        MemoryPolicyConfig config = GSON.fromJson(merged, MemoryPolicyConfig.class);
        if (config.writeback != null && Boolean.TRUE.equals(config.writeback.enabled)) {
            if (config.zram == null || !Boolean.TRUE.equals(config.zram.enabled)
                    || config.writeback.backendResourceUuid == null || config.writeback.backendCapacityBytes == null) {
                throw invalid("writeback", "requires enabled zram and explicit backend resource/capacity");
            }
        }
        if (config.zram != null && config.zram.ramLimitBytes != null && config.zram.logicalCapacityBytes != null
                && config.zram.ramLimitBytes > config.zram.logicalCapacityBytes) {
            throw invalid("ramLimitBytes", "must not exceed logicalCapacityBytes");
        }
        return merged;
    }

    /** Structural merge for one scope's sparse overrides; cross-scope constraints are checked on effective policy. */
    public static String mergeOverridesOnly(String current, String patch, String scope) {
        if (!Arrays.asList("Global", "Cluster", "Host", "VM").contains(scope)) {
            throw invalid("scope", "must be Global, Cluster, Host or VM");
        }
        JsonObject result = object(current, "current");
        JsonObject updates = object(patch, "policy");
        validate(updates, scope);
        for (Map.Entry<String, JsonElement> entry : updates.entrySet()) {
            if (entry.getValue().isJsonObject() && result.has(entry.getKey())) {
                JsonObject target = result.getAsJsonObject(entry.getKey());
                entry.getValue().getAsJsonObject().entrySet().forEach(e -> target.add(e.getKey(), e.getValue()));
            } else {
                result.add(entry.getKey(), entry.getValue());
            }
        }
        validate(result, scope);
        return canonical(result).toString();
    }

    public static MemoryPolicyConfig decode(String value) {
        return GSON.fromJson(value, MemoryPolicyConfig.class);
    }

    /** Only for durable pre-upgrade records, never for incoming API requests. */
    static String migrateLegacy(String current, String scope) {
        JsonObject result = object(current, "current");
        if (!result.has("ksm") || !result.get("ksm").isJsonObject()) { return current; }
        JsonObject ksm = result.getAsJsonObject("ksm");
        boolean changed = false;
        for (String field : Arrays.asList("mode", "profileVersion", "cpuBudgetPercent")) {
            changed |= ksm.remove(field) != null;
        }
        if (!changed) { return current; }
        // No enabled/default is synthesized. In particular Legacy/untaken
        // ownership cannot turn into a request to enable optimization.
        validate(result, scope);
        return canonical(result).toString();
    }

    public static String clearFields(String current, List<String> paths, String scope) {
        if (paths == null || paths.isEmpty()) {
            throw invalid("clearOverrideFields", "must explicitly identify fields to inherit");
        }
        JsonObject result = object(current, "current");
        for (String path : paths) {
            if ("VM".equals(scope) && "participation".equals(path)) {
                result.remove(path);
                continue;
            }
            String[] parts = path == null ? new String[0] : path.split("\\.", -1);
            Set<String> fields = parts.length != 2 ? null : "ksm".equals(parts[0]) ? KSM
                    : "zram".equals(parts[0]) ? ZRAM : "writeback".equals(parts[0]) ? WRITEBACK : null;
            if ("VM".equals(scope) || fields == null || !fields.contains(parts[1])) {
                throw invalid("clearOverrideFields", "unknown field: " + path);
            }
            if (result.has(parts[0])) {
                result.getAsJsonObject(parts[0]).remove(parts[1]);
            }
        }
        // A partial override need not be self-contained; its effective policy is
        // validated together with its parents before any task is dispatched.
        validate(result, scope);
        return canonical(result).toString();
    }

    public static void addFieldSources(String policy, String source, Map<String, String> result) {
        object(policy, "policy").entrySet().forEach(section -> {
            if (section.getValue().isJsonObject()) {
                section.getValue().getAsJsonObject().entrySet().forEach(field ->
                        result.put(section.getKey() + "." + field.getKey(), source));
            } else if (!"schemaVersion".equals(section.getKey())) {
                result.put(section.getKey(), source);
            }
        });
    }

    public static String canonicalRequest(Object value) {
        return canonical(GSON.toJsonTree(value)).toString();
    }

    private static JsonElement canonical(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject result = new JsonObject();
            TreeMap<String, JsonElement> sorted = new TreeMap<>();
            value.getAsJsonObject().entrySet().forEach(e -> sorted.put(e.getKey(), e.getValue()));
            sorted.forEach((key, item) -> result.add(key, canonical(item)));
            return result;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray();
            value.getAsJsonArray().forEach(item -> result.add(canonical(item)));
            return result;
        }
        return value;
    }

    private static JsonObject object(String json, String name) {
        try {
            JsonElement result = new JsonParser().parse(json);
            if (!result.isJsonObject()) {
                throw invalid(name, "must be an object");
            }
            return result.getAsJsonObject();
        } catch (JsonParseException | NullPointerException e) {
            throw invalid(name, "invalid JSON object");
        }
    }

    private static void validate(JsonObject value, String scope) {
        for (Map.Entry<String, JsonElement> entry : value.entrySet()) {
            String key = entry.getKey();
            JsonElement item = entry.getValue();
            if (item == null || item.isJsonNull()) {
                throw invalid(key, "null is not a clear operation; use clearOverride");
            }
            if ("schemaVersion".equals(key)) {
                integer(item, key, 1, 1, false);
            } else if ("VM".equals(scope)) {
                if (!"participation".equals(key)) {
                    throw invalid(key, "VM policy only supports participation");
                }
                enumeration(item, key, "inherit", "allow", "deny");
            } else {
                Set<String> fields = "ksm".equals(key) ? KSM : "zram".equals(key) ? ZRAM
                        : "writeback".equals(key) ? WRITEBACK : null;
                if (fields == null || !item.isJsonObject()) {
                    throw invalid(key, "unknown policy section or non-object value");
                }
                validateSection(key, item.getAsJsonObject(), fields);
            }
        }
    }

    private static void validateSection(String section, JsonObject value, Set<String> fields) {
        for (Map.Entry<String, JsonElement> entry : value.entrySet()) {
            String key = entry.getKey();
            JsonElement item = entry.getValue();
            if ("selectedVmUuids".equals(key) && fields.contains(key)) {
                if (!item.isJsonArray()) { throw invalid(key, "must be a UUID array"); }
                Set<String> seen = new HashSet<>();
                for (JsonElement id : item.getAsJsonArray()) {
                    if (!id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString() || !validUuid(id.getAsString())
                            || !seen.add(id.getAsString().replace("-", "").toLowerCase(Locale.ROOT))) {
                        throw invalid(key, "contains an invalid or duplicate UUID");
                    }
                }
                continue;
            }
            if (!fields.contains(key) || item == null || !item.isJsonPrimitive()) {
                throw invalid(section + "." + key, "unknown field, null or invalid type");
            }
            if (key.endsWith("Enabled") || "enabled".equals(key)) {
                if (!item.getAsJsonPrimitive().isBoolean()) {
                    throw invalid(key, "must be boolean");
                }
            } else if ("selectionMode".equals(key)) {
                enumeration(item, key, "all_running", "list");
            } else if ("algorithm".equals(key)) {
                if (!item.getAsJsonPrimitive().isString() || !item.getAsString().matches("[a-zA-Z0-9_-]{1,64}")) {
                    throw invalid(key, "must be a kernel-advertised algorithm identifier");
                }
            } else if ("backendResourceUuid".equals(key)) {
                if (!item.getAsJsonPrimitive().isString() || !validUuid(item.getAsString())) {
                    throw invalid(key, "requires a resource UUID, not a path");
                }
            } else {
                validateNumber(key, item);
            }
        }
    }

    private static void validateNumber(String key, JsonElement item) {
        if (key.endsWith("Bytes")) {
            boolean allocation = Arrays.asList("logicalCapacityBytes", "ramLimitBytes",
                    "backendCapacityBytes", "batchBytes", "reclaimBatchBytes").contains(key);
            boolean positive = key.equals("operationRecordBudgetBytes") || key.equals("metadataBudgetBytes");
            integer(item, key, allocation ? 4096 : positive ? 1 : 0, Long.MAX_VALUE, allocation);
        } else if (key.startsWith("host") && key.endsWith("ThresholdPercent")) {
            if (!item.getAsJsonPrimitive().isNumber()) { throw invalid(key, "must be a percentage number"); }
            try {
                BigDecimal percentage = new BigDecimal(item.getAsString());
                if (percentage.signum() < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw invalid(key, "must be between 0 and 100");
                }
            } catch (NumberFormatException e) { throw invalid(key, "must be a finite percentage"); }
        } else if (key.endsWith("Percent")) {
            integer(item, key, 1, 100, false);
        } else if ("pagesToScan".equals(key)) {
            integer(item, key, 1, UINT_MAX, false);
        } else if ("sleepMillis".equals(key)) {
            // The retained original ksmtuned enforces a 10ms minimum; the
            // host adapter still checks exact representability after scaling.
            integer(item, key, 10, UINT_MAX, false);
        } else if (Arrays.asList("concurrency", "timeoutIsolationSlots", "noProgressLimit").contains(key)) {
            integer(item, key, "timeoutIsolationSlots".equals(key) ? 0 : 1, Integer.MAX_VALUE, false);
        } else {
            integer(item, key, "startupObservationSeconds".equals(key) ? 0 : 1, Long.MAX_VALUE / 1000000000L, false);
        }
    }

    private static void integer(JsonElement value, String field, long min, long max, boolean aligned) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw invalid(field, "must be an integer number");
        }
        try {
            long number = new BigDecimal(value.getAsString()).longValueExact();
            if (number < min || number > max || (aligned && number % 4096 != 0)) {
                throw invalid(field, "out of range or not 4KiB aligned");
            }
        } catch (ArithmeticException | NumberFormatException e) {
            throw invalid(field, "must be a bounded int64");
        }
    }

    private static void enumeration(JsonElement value, String key, String... allowed) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || !Arrays.asList(allowed).contains(value.getAsString())) {
            throw invalid(key, "invalid enum value");
        }
    }

    public static boolean validUuid(String value) {
        return value != null && (value.matches("[a-fA-F0-9]{32}")
                || value.matches("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}"));
    }

    private static IllegalArgumentException invalid(String key, String reason) {
        return new IllegalArgumentException("MEMORY_INVALID_POLICY: " + key + " " + reason);
    }
}
