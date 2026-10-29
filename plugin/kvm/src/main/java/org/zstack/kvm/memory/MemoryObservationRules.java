package org.zstack.kvm.memory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.zstack.header.host.HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/** Export only the requested VM. Missing or stale attribution never becomes zero. */
public final class MemoryObservationRules {
    private static final CLogger logger = Utils.getLogger(MemoryObservationRules.class);
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

    /**
     * Service failures and recoveries of a fresh sample on an active Host, as
     * changes from the previous sample. A paused or drained Host stops its
     * services on purpose.
     */
    static List<HostMemoryOptimizationServiceStateChangedData> serviceEvents(String host,
            MemoryAgentResponse response, MemoryStateVO current, String previousSample, long now) {
        List<HostMemoryOptimizationServiceStateChangedData> result = new ArrayList<>();
        if (current == null || current.getDesired() == null || response == null || !response.isSuccess()
                || response.state == null || response.hostUuid != null && !host.equals(response.hostUuid)) { return result; }
        Map<String, Object> state = response.state;
        Long sampled = nullableLong(state.get("sampleTime"));
        long ttl = MemoryOptimizationGlobalConfig.displayTtlMillis();
        if (sampled == null || sampled > now + 5000 || now - sampled >= ttl || !"active".equals(state.get("control"))) {
            return result;
        }
        Map<?, ?> desired;
        try { desired = org.zstack.utils.gson.JSONObjectUtil.toObject(current.getDesired(), java.util.LinkedHashMap.class); }
        catch (RuntimeException invalid) {
            logger.error(String.format("Host[%s] stored memory configuration is unreadable; its service events are not reported", host), invalid);
            return result;
        }
        if (!"active".equals(desired.get("control"))) { return result; }
        Map<?, ?> previous;
        try { previous = previousSample == null ? null : org.zstack.utils.gson.JSONObjectUtil.toObject(previousSample, java.util.LinkedHashMap.class); }
        catch (RuntimeException invalid) {
            logger.warn(String.format("Host[%s] previous memory sample is unreadable; this sample is compared with none", host), invalid);
            previous = null;
        }
        Map<?, ?> ksm = map(state.get("ksm"));
        Map<?, ?> previousKsm = previous == null ? null : map(previous.get("ksm"));
        if (Boolean.TRUE.equals(field(desired.get("ksm"), "enabled")) && ksm != null) {
            nativeServiceEvent(result, host, "ksm", ksm.get("serviceState"), field(previousKsm, "serviceState"));
            nativeServiceEvent(result, host, "ksmtuned", ksm.get("tunedServiceState"), field(previousKsm, "tunedServiceState"));
        }
        Map<?, ?> status = map(field(state.get("zram"), "status"));
        if (Boolean.TRUE.equals(field(desired.get("zram"), "enabled"))
                && Boolean.TRUE.equals(field(desired.get("writeback"), "enabled"))
                && status != null && Boolean.TRUE.equals(status.get("resource_authorized"))
                && observationFailure(status.get("observed_at"), now, ttl) == null) {
            Map<?, ?> health = map(status.get("backend_health"));
            Map<?, ?> previousStatus = map(field(previous == null ? null : previous.get("zram"), "status"));
            Map<?, ?> previousHealth = previousStatus == null ? null : map(previousStatus.get("backend_health"));
            if (writebackFailed(health) && !writebackFailed(previousHealth)) {
                result.add(serviceEvent(host, "zram-writeback", "Failed", "NATIVE_BACKEND_IO_ERROR_BACKOFF"));
            } else if (health != null && writebackRecovered(status, health)
                    && !(previousHealth != null && writebackRecovered(previousStatus, previousHealth))) {
                result.add(serviceEvent(host, "zram-writeback", "Recovered", "WRITEBACK_IO_SUCCEEDED_AFTER_BACKOFF"));
            }
        }
        return result;
    }

    private static Object field(Object value, String key) {
        return value instanceof Map ? ((Map<?, ?>) value).get(key) : null;
    }

    private static void nativeServiceEvent(List<HostMemoryOptimizationServiceStateChangedData> events,
                                           String host, String service, Object state, Object before) {
        if ("failed".equals(state) && !"failed".equals(before)) { events.add(serviceEvent(host, service, "Failed", "SYSTEMD_FAILED")); }
        else if ("active".equals(state) && "failed".equals(before)) { events.add(serviceEvent(host, service, "Recovered", "SYSTEMD_ACTIVE")); }
    }

    private static HostMemoryOptimizationServiceStateChangedData serviceEvent(String host, String service,
                                                                             String status, String reason) {
        HostMemoryOptimizationServiceStateChangedData result = new HostMemoryOptimizationServiceStateChangedData();
        result.setHostUuid(host); result.setService(service); result.setStatus(status); result.setReason(reason);
        return result;
    }

    private static boolean writebackFailed(Map<?, ?> health) {
        return health != null && "DEGRADED".equals(health.get("state"))
                && "NATIVE_BACKEND_IO_ERROR_BACKOFF".equals(health.get("reason"))
                && Boolean.FALSE.equals(health.get("new_io_permitted_by_backend_health"));
    }

    private static boolean writebackRecovered(Map<?, ?> state, Map<?, ?> health) {
        if (!"READY".equals(health.get("state"))
                || !"BACKOFF_ELAPSED_BOUNDED_IO_PERMITTED".equals(health.get("reason"))
                || !Boolean.TRUE.equals(health.get("new_io_permitted_by_backend_health"))) { return false; }
        Map<?, ?> returned = map(state.get("last_writeback_return"));
        Map<?, ?> schedule = map(state.get("writeback_schedule"));
        Map<?, ?> pool = map(state.get("host_zram"));
        if (returned == null || schedule == null || pool == null
                || !Boolean.TRUE.equals(returned.get("kernel_called"))
                || returned.get("error") != null && !"".equals(returned.get("error"))
                || !(returned.get("operation_id") instanceof String)
                || ((String) returned.get("operation_id")).isEmpty()
                || !returned.get("operation_id").equals(schedule.get("last_completed_id"))
                || !(pool.get("pool_generation") instanceof String)
                || ((String) pool.get("pool_generation")).isEmpty()
                || !pool.get("pool_generation").equals(schedule.get("pool_generation"))
                || !"native_host_device".equals(pool.get("quality"))) { return false; }
        Map<?, ?> before = map(returned.get("before"));
        Map<?, ?> after = map(returned.get("after"));
        if (before == null || after == null || !"native_host_device".equals(before.get("quality"))
                || !"native_host_device".equals(after.get("quality"))) { return false; }
        try {
            java.time.Instant until = java.time.OffsetDateTime.parse((String) health.get("backoff_until")).toInstant();
            java.time.Instant started = java.time.OffsetDateTime.parse((String) before.get("observed_at")).toInstant();
            java.time.Instant ended = java.time.OffsetDateTime.parse((String) returned.get("returned_at")).toInstant();
            java.time.Instant observed = java.time.OffsetDateTime.parse((String) state.get("observed_at")).toInstant();
            // idle marking also has a successful return: require real committed writeback progress.
            Long oldPages = nullableLong(before.get("committed_writeback_pages_total"));
            Long newPages = nullableLong(after.get("committed_writeback_pages_total"));
            if (oldPages == null || oldPages < 0 || newPages == null || newPages <= oldPages
                    || !started.isAfter(until) || ended.isBefore(started) || observed.isBefore(ended)) { return false; }
            for (String counter : new String[]{"read_errors", "write_errors"}) {
                Long previous = nullableLong(before.get(counter));
                Long current = nullableLong(after.get(counter));
                if (previous == null || previous < 0 || !previous.equals(current)) { return false; }
            }
            return true;
        } catch (RuntimeException malformed) { return false; }
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map ? (Map<?, ?>) value : null;
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
