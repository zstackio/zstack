package org.zstack.kvm.memory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.zstack.header.host.HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.gson.JSONObjectUtil;

/** Service failures and recoveries reported by a Host memory sample. */
public final class MemoryObservationRules {
    private static final long MAX_SAFE_DOUBLE_INTEGER = 9007199254740991L;
    private static final int MAX_SAFE_FLOAT_INTEGER = 16777215;

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
        Long sampled = exactLong(state.get("sampleTime"));
        long ttl = MemoryOptimizationGlobalConfig.displayTtlMillis();
        if (sampled == null || sampled > now + 5000 || now - sampled >= ttl || !"active".equals(state.get("control"))) {
            return result;
        }
        // An unreadable configuration is planned again; until then its services are not compared.
        Map<?, ?> desired = readJson(current.getDesired(), java.util.LinkedHashMap.class, String.format(
                "Host[%s] stored memory configuration is unreadable; its service events are not reported", host));
        if (desired == null) { return result; }
        if (!"active".equals(desired.get("control"))) { return result; }
        // This sample replaces an unreadable one, so the next comparison is exact again.
        Map<?, ?> previous = previousSample == null ? null : readJson(previousSample, java.util.LinkedHashMap.class,
                String.format("Host[%s] previous memory sample is unreadable; this sample is compared with none", host));
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
        Long until = epochMillis(health.get("backoff_until"));
        Long started = epochMillis(before.get("observed_at"));
        Long ended = epochMillis(returned.get("returned_at"));
        Long observed = epochMillis(state.get("observed_at"));
        // idle marking also has a successful return: require real committed writeback progress.
        Long oldPages = exactLong(before.get("committed_writeback_pages_total"));
        Long newPages = exactLong(after.get("committed_writeback_pages_total"));
        if (until == null || started == null || ended == null || observed == null
                || oldPages == null || oldPages < 0 || newPages == null || newPages <= oldPages
                || started <= until || ended < started || observed < ended) { return false; }
        for (String counter : new String[]{"read_errors", "write_errors"}) {
            Long previous = exactLong(before.get(counter));
            Long current = exactLong(after.get(counter));
            if (previous == null || previous < 0 || !previous.equals(current)) { return false; }
        }
        return true;
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map ? (Map<?, ?>) value : null;
    }

    private static final java.util.regex.Pattern DECIMAL = java.util.regex.Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d{1,4})?");
    private static final java.util.regex.Pattern RFC3339 = java.util.regex.Pattern.compile(
            "(\\d{4})-(\\d{2})-(\\d{2})[Tt](\\d{2}):(\\d{2})(?::(\\d{2})(\\.\\d{1,9})?)?([Zz]|([+-])(\\d{2}):(\\d{2}))");

    /** The value a JSON text holds, or null when it cannot be read; why is logged with the context. */
    static <T> T readJson(String text, Class<T> type, String context) {
        List<T> read = new ArrayList<>(1);
        ExceptionDSL.exceptionSafe(() -> read.add(JSONObjectUtil.toObject(text, type)), context);
        return read.isEmpty() ? null : read.get(0);
    }

    /** The exact signed long a JSON number holds, or null when it is absent or not an exact integer. */
    static Long exactLong(Object value) {
        if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return ((Number) value).longValue();
        }
        if (value instanceof Double || value instanceof Float) {
            double number = ((Number) value).doubleValue();
            double safe = value instanceof Float ? MAX_SAFE_FLOAT_INTEGER : MAX_SAFE_DOUBLE_INTEGER;
            // Signed long is intentional: do not clamp valid negative differences/savings to zero.
            return Double.isFinite(number) && number == Math.rint(number) && Math.abs(number) <= safe ? (long) number : null;
        }
        if (!(value instanceof Number) || !DECIMAL.matcher(value.toString()).matches()) { return null; }
        BigDecimal number = new BigDecimal(value.toString());
        if (number.signum() == 0) { return 0L; }
        boolean whole = number.stripTrailingZeros().scale() <= 0;
        boolean inRange = number.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) >= 0
                && number.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0;
        return whole && inRange ? number.longValue() : null;
    }

    /** The exact long a decimal text spells, or null when it is not one. */
    static Long parseExactLong(String text) {
        return text != null && DECIMAL.matcher(text).matches() ? exactLong(new BigDecimal(text)) : null;
    }

    /** The epoch milliseconds of an RFC 3339 time (seconds may be left out), or null when the value is not one. */
    public static Long epochMillis(Object value) {
        java.util.regex.Matcher time = value instanceof String ? RFC3339.matcher((String) value) : null;
        if (time == null || !time.matches()) { return null; }
        int year = Integer.parseInt(time.group(1)), month = Integer.parseInt(time.group(2)), day = Integer.parseInt(time.group(3));
        // Java's ISO-8601 text leaves out zero seconds.
        int hour = Integer.parseInt(time.group(4)), minute = Integer.parseInt(time.group(5));
        int second = time.group(6) == null ? 0 : Integer.parseInt(time.group(6));
        if (month < 1 || month > 12 || day < 1 || day > java.time.YearMonth.of(year, month).lengthOfMonth()
                || hour > 23 || minute > 59 || second > 59) { return null; }
        String fraction = time.group(7) == null ? "" : time.group(7).substring(1);
        int nanos = fraction.isEmpty() ? 0 : Integer.parseInt((fraction + "00000000").substring(0, 9));
        int offsetSeconds = 0;
        if (time.group(9) != null) {
            int offsetHours = Integer.parseInt(time.group(10)), offsetMinutes = Integer.parseInt(time.group(11));
            if (offsetHours > 18 || offsetMinutes > 59 || offsetHours == 18 && offsetMinutes > 0) { return null; }
            offsetSeconds = ("-".equals(time.group(9)) ? -1 : 1) * (offsetHours * 3600 + offsetMinutes * 60);
        }
        return java.time.OffsetDateTime.of(year, month, day, hour, minute, second, nanos,
                java.time.ZoneOffset.ofTotalSeconds(offsetSeconds)).toInstant().toEpochMilli();
    }

    private static String observationFailure(Object value, long now, long ttl) {
        if (!(value instanceof String)) {
            return "SAMPLE_TIME_MISSING";
        }
        Long sampled = epochMillis(value);
        if (sampled == null) {
            return "SAMPLE_TIME_INVALID";
        }
        if (sampled > now + 5000) {
            return "SAMPLE_TIME_AHEAD";
        }
        return now - sampled >= ttl ? "SAMPLE_EXPIRED" : null;
    }
}
