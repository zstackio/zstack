package org.zstack.kvm.memory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/** Export only the requested VM. Missing or stale attribution never becomes zero. */
public final class MemoryObservationRules {
    private static final long MAX_SAFE_DOUBLE_INTEGER = 9007199254740991L;
    private static final int MAX_SAFE_FLOAT_INTEGER = 16777215;
    private static final BigInteger UINT64_MAX = new BigInteger("18446744073709551615");
    private static final String[] LONG_METRICS = {
            "compressed_pages", "incompressible_pages", "zero_pages", "nonzero_same_pages",
            "backend_committed_pages", "ram_payload_bytes", "ram_original_bytes",
            "backend_committed_original_bytes", "ram_logical_minus_payload_bytes"
    };

    private MemoryObservationRules() {
    }

    public static MemoryVmAccountingInventory vm(String host, String vm, Map<String, Object> report) {
        return vm(host, vm, report, System.currentTimeMillis(), MemoryOptimizationGlobalConfig.displayTtlMillis());
    }

    static MemoryVmAccountingInventory vm(String host, String vm, Map<String, Object> report,
                                          long now, long ttl) {
        MemoryVmAccountingInventory result = new MemoryVmAccountingInventory();
        result.setHostUuid(host);
        result.setVmUuid(vm);
        result.setDisplayTtlMillis(ttl);
        result.setObservedAt(stringOrNull(report.get("observed_at")));
        result.setHostBootId(stringOrNull(report.get("host_boot_id")));
        result.setDevice(stringOrNull(report.get("device")));
        if (invalidInteger(report.get("schema_version"))) {
            return unavailable(result, "OBSERVATION_METADATA_INVALID");
        }
        result.setSchemaVersion(intOrNull(report.get("schema_version")));
        result.setOwnershipSemantics(stringOrNull(report.get("ownership_semantics")));

        Map<?, ?> inventory = map(report.get("inventory"));
        Object generation = inventory == null ? null : inventory.get("pool_generation");
        Object sequence = inventory == null ? null : inventory.get("sequence");
        if (unsafeIdentity(generation) || unsafeIdentity(sequence)) {
            return unavailable(result, "IDENTITY_TOKEN_UNSAFE");
        }
        result.setPoolGeneration(identityValue(generation));
        result.setSequence(identityValue(sequence));

        Map<?, ?> vms = map(report.get("vms"));
        Map<?, ?> entry = vms == null ? null : map(vms.get(vm));
        // Prometheus may return different observation times for different VMs.
        // Keep each sample's original time; request time is never a sample time.
        Object sampled = entry != null && entry.containsKey("observed_at")
                ? entry.get("observed_at") : report.get("observed_at");
        result.setObservedAt(stringOrNull(sampled));

        String failure = observationFailure(sampled, now, ttl);
        if (entry != null && failure != null) {
            result.setQuality("SAMPLE_EXPIRED".equals(failure) ? "stale" : "unavailable");
            result.setReason(failure);
            return result;
        }
        if (entry == null) {
            result.setQuality("unsupported".equals(report.get("quality")) ? "unsupported" : "unavailable");
            result.setReason(defaultReason(report.get("reason"), "VM_ACCOUNTING_NOT_AVAILABLE"));
            return result;
        }

        result.setQuality(stringOrNull(entry.get("quality")));
        result.setReason(stringOrNull(entry.get("reason")));
        if (invalidInteger(entry.get("pid"))) {
            return unavailable(result, "VM_IDENTITY_INVALID");
        }

        Map<?, ?> identity = map(entry.get("identity"));
        if (identity != null) {
            if (invalidInteger(identity.get("pid"))) {
                return unavailable(result, "VM_IDENTITY_INVALID");
            }
            Object identityGeneration = identity.get("pool_generation");
            if (unsafeIdentity(identityGeneration)
                    || unsafeIdentity(identity.get("sequence"))
                    || unsafeIdentity(identity.get("cgroup_id"))
                    || unsafeIdentity(identity.get("cgroup_inode"))
                    || unsafeIdentity(identity.get("mm_context_id"))
                    || unsafeIdentity(identity.get("start_time"))
                    || unsafeIdentity(identity.get("sampled_monotonic_ns"))) {
                return unavailable(result, "IDENTITY_TOKEN_UNSAFE");
            }
            if (identityGeneration != null) {
                result.setPoolGeneration(identityValue(identityGeneration));
            }
            MemoryVmIdentityInventory projected = projectIdentity(identity, identityGeneration);
            result.setIdentity(projected);
            // Keep the old top-level aliases while the typed identity object is adopted.
            if (projected.getHostBootId() != null) {
                result.setHostBootId(projected.getHostBootId());
            }
            if (projected.getDevice() != null) {
                result.setDevice(projected.getDevice());
            }
        } else if (entry.get("identity") != null) {
            return unavailable(result, "VM_IDENTITY_MALFORMED");
        }

        Object rawMetrics = entry.get("metrics");
        if (rawMetrics == null) {
            // The legacy map returned a null metrics value for unsupported and
            // unavailable rows. Preserve their original quality/reason; absence
            // alone is not evidence that the Agent sent a malformed object.
            return result;
        }
        Map<?, ?> metrics = map(rawMetrics);
        if (metrics == null) {
            return unavailable(result, "VM_ACCOUNTING_METRICS_MALFORMED");
        }

        MemoryVmAccountingMetricsInventory projectedMetrics = new MemoryVmAccountingMetricsInventory();
        try {
            for (String key : LONG_METRICS) {
                setLongMetric(projectedMetrics, key, nullableLong(metrics.get(key)));
            }
            projectedMetrics.setRamLogicalToPayloadRatio(nullableDouble(metrics.get("ram_logical_to_payload_ratio")));
            projectedMetrics.setRatioState(stringOrNull(metrics.get("ratio_state")));
        } catch (IllegalArgumentException invalidMetric) {
            return unavailable(result, "VM_ACCOUNTING_METRICS_INVALID");
        }
        result.setMetrics(projectedMetrics);
        return result;
    }

    private static MemoryVmIdentityInventory projectIdentity(Map<?, ?> source, Object generation) {
        MemoryVmIdentityInventory result = new MemoryVmIdentityInventory();
        result.setUuid(stringOrNull(source.get("uuid")));
        result.setHostBootId(stringOrNull(source.get("host_boot_id")));
        result.setInstanceGeneration(stringOrNull(source.get("instance_generation")));
        result.setMmContextId(identityToken(source.get("mm_context_id")));
        result.setCgroupId(identityToken(source.get("cgroup_id")));
        result.setStartTime(identityToken(source.get("start_time")));
        result.setSampledMonotonicNs(identityToken(source.get("sampled_monotonic_ns")));
        result.setCgroupInode(identityToken(source.get("cgroup_inode")));
        result.setSequence(identityToken(source.get("sequence")));
        result.setCgroupPath(stringOrNull(source.get("cgroup_path")));
        result.setPoolGeneration(generation == null ? null : identityValue(generation));
        result.setDevice(stringOrNull(source.get("device")));
        result.setPid(intOrNull(source.get("pid")));
        return result;
    }

    private static void setLongMetric(MemoryVmAccountingMetricsInventory inventory, String key, Long value) {
        switch (key) {
            case "compressed_pages":
                inventory.setCompressedPages(value);
                break;
            case "incompressible_pages":
                inventory.setIncompressiblePages(value);
                break;
            case "zero_pages":
                inventory.setZeroPages(value);
                break;
            case "nonzero_same_pages":
                inventory.setNonzeroSamePages(value);
                break;
            case "backend_committed_pages":
                inventory.setBackendCommittedPages(value);
                break;
            case "ram_payload_bytes":
                inventory.setRamPayloadBytes(value);
                break;
            case "ram_original_bytes":
                inventory.setRamOriginalBytes(value);
                break;
            case "backend_committed_original_bytes":
                inventory.setBackendCommittedOriginalBytes(value);
                break;
            case "ram_logical_minus_payload_bytes":
                inventory.setRamLogicalMinusPayloadBytes(value);
                break;
            default:
                throw new IllegalArgumentException("Unknown metric " + key);
        }
    }

    private static Long nullableLong(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Metric is not numeric");
        }
        try {
            if (value instanceof Double) {
                double number = (Double) value;
                if (!Double.isFinite(number) || number != Math.rint(number)
                        || Math.abs(number) > MAX_SAFE_DOUBLE_INTEGER) {
                    throw new IllegalArgumentException("Unsafe floating-point metric");
                }
            } else if (value instanceof Float) {
                float number = (Float) value;
                if (!Float.isFinite(number) || number != Math.rint(number)
                        || Math.abs(number) > MAX_SAFE_FLOAT_INTEGER) {
                    throw new IllegalArgumentException("Unsafe floating-point metric");
                }
            }
            // Signed long is intentional: do not clamp valid negative differences/savings to zero.
            return new BigDecimal(value.toString()).longValueExact();
        } catch (ArithmeticException | NumberFormatException invalidNumber) {
            throw new IllegalArgumentException("Metric is not an exact signed long", invalidNumber);
        }
    }

    private static Double nullableDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Ratio is not numeric");
        }
        double number = ((Number) value).doubleValue();
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("Ratio is non-finite");
        }
        return number;
    }

    private static Integer intOrNull(Object value) {
        if (value == null || !(value instanceof Number) || invalidInteger(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException invalidNumber) {
            return null;
        }
    }

    private static boolean invalidInteger(Object value) {
        if (value == null) {
            return false;
        }
        if (!(value instanceof Number)) {
            return true;
        }
        try {
            if (value instanceof Double) {
                double number = (Double) value;
                if (!Double.isFinite(number) || number != Math.rint(number)) {
                    return true;
                }
            }
            if (value instanceof Float) {
                float number = (Float) value;
                if (!Float.isFinite(number) || number != Math.rint(number)) {
                    return true;
                }
            }
            new BigDecimal(value.toString()).intValueExact();
            return false;
        } catch (ArithmeticException | NumberFormatException invalidNumber) {
            return true;
        }
    }

    private static String stringOrNull(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private static String defaultReason(Object value, String fallback) {
        String reason = stringOrNull(value);
        return reason == null ? fallback : reason;
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map ? (Map<?, ?>) value : null;
    }

    private static MemoryVmAccountingInventory unavailable(MemoryVmAccountingInventory inventory, String reason) {
        inventory.setQuality("unavailable");
        inventory.setReason(reason);
        inventory.setMetrics(null);
        inventory.setIdentity(null);
        return inventory;
    }

    private static String identityToken(Object value) {
        if (value == null) {
            return null;
        }
        if (unsafeIdentity(value)) {
            throw new IllegalArgumentException("IDENTITY_TOKEN_UNSAFE");
        }
        return identityValue(value);
    }

    private static String identityValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        if (!(value instanceof Number)) {
            return null;
        }
        if (value instanceof Float || value instanceof Double) {
            double number = ((Number) value).doubleValue();
            if (!Double.isFinite(number) || number != Math.rint(number) || number < 0
                    || number > MAX_SAFE_DOUBLE_INTEGER) {
                return null;
            }
            return Long.toString((long) number);
        }
        try {
            BigInteger number = new BigDecimal(value.toString()).toBigIntegerExact();
            return number.signum() < 0 ? null : number.toString();
        } catch (ArithmeticException | NumberFormatException invalidNumber) {
            return null;
        }
    }

    private static boolean unsafeIdentity(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            // The Agent wire adapter decimal-stringifies uint64 tokens. A String
            // generation label is intentionally handled separately as opaque text.
            String token = (String) value;
            if (!token.matches("(?:0|[1-9][0-9]*)")) {
                return true;
            }
            return exceedsUint64(new BigDecimal(token));
        }
        if (!(value instanceof Number)) {
            return true;
        }
        if (value instanceof Float) {
            float number = ((Number) value).floatValue();
            return !Float.isFinite(number) || number != Math.rint(number) || number < 0
                    || number > MAX_SAFE_FLOAT_INTEGER;
        }
        if (value instanceof Double) {
            double number = ((Number) value).doubleValue();
            // Unsafe IEEE-754 values have already lost uint64 bits; never publish a rounded identity.
            return !Double.isFinite(number) || number != Math.rint(number) || number < 0
                    || number > MAX_SAFE_DOUBLE_INTEGER;
        }
        try {
            BigDecimal number = new BigDecimal(value.toString());
            return number.signum() < 0 || exceedsUint64(number);
        } catch (NumberFormatException invalidNumber) {
            return true;
        }
    }

    private static boolean exceedsUint64(BigDecimal value) {
        try {
            return value.toBigIntegerExact().compareTo(UINT64_MAX) > 0;
        } catch (ArithmeticException nonInteger) {
            return true;
        }
    }

    private static String observationFailure(Object value, long now, long ttl) {
        if (!(value instanceof String)) {
            return "SAMPLE_TIME_MISSING";
        }
        try {
            long sampled;
            try {
                sampled = java.time.Instant.parse((String) value).toEpochMilli();
            } catch (java.time.DateTimeException utcParseFailure) {
                // Some supported JDK 8 builds parse RFC3339 offsets more reliably through OffsetDateTime.
                sampled = java.time.OffsetDateTime.parse((String) value).toInstant().toEpochMilli();
            }
            if (sampled > now + 5000) {
                return "SAMPLE_TIME_IN_FUTURE";
            }
            return now - sampled >= ttl ? "SAMPLE_EXPIRED" : null;
        } catch (java.time.DateTimeException | ArithmeticException invalid) {
            return "SAMPLE_TIME_INVALID";
        }
    }
}
