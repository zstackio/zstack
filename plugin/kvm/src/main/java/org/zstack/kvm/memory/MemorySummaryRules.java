package org.zstack.kvm.memory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.util.*;

/** Aggregate complete same-window Host samples; never join independent metric peaks. */
public final class MemorySummaryRules {
    private MemorySummaryRules() { }
    public static MemorySummaryInventory summarize(List<String> hosts, List<MemoryStateVO> states, long now) {
        Set<String> expected = new HashSet<>(hosts);
        Map<String, HostMetrics> samples = new HashMap<>();
        Map<String, Integer> currentStatus = new LinkedHashMap<>();
        long windowMillis = MemoryOptimizationGlobalConfig.summaryWindowMillis();
        long bucket = -1;
        Map<String, MemoryStateVO> byHost = new HashMap<>();
        for (MemoryStateVO host : states) {
            if (!expected.contains(host.getHostUuid())) { continue; }
            byHost.put(host.getHostUuid(), host);
            if (host.getState() == null) { continue; }
            try {
                JsonObject json = new JsonParser().parse(host.getState()).getAsJsonObject();
                if (!json.has("savings") || !json.get("savings").isJsonObject()) { continue; }
                JsonObject savings = json.getAsJsonObject("savings");
                // A POJO default must never turn an absent producer schema into a supported one.
                if (!savings.has("formulaVersion") || !savings.has("quality")) { continue; }
                HostMetrics sample = parseMetrics(savings, now);
                if (sample == null) { continue; }
                bucket = Math.max(bucket, sample.sampleTime / windowMillis);
                samples.put(host.getHostUuid(), sample);
            } catch (RuntimeException e) { /* Malformed/unavailable samples remain missing. */ }
        }
        MemorySummaryInventory result = new MemorySummaryInventory();
        result.setExpectedHosts(expected.size()); result.setQuality("Unknown");
        for (String host : expected) {
            if (!byHost.containsKey(host)) { increment(currentStatus, "unknown"); }
            else { classifyCurrentStatus(byHost.get(host), now, currentStatus); }
        }
        result.setCurrentStatus(currentStatus);
        result.setCurrentStatusSampleTime(now);
        result.setCurrentStatusTtlMillis(MemoryOptimizationGlobalConfig.displayTtlMillis());
        long ordinary = 0, zero = 0, subtotal = 0, zram = 0, total = 0;
        long zramNormal = 0, zramWriteback = 0;
        int count = 0, ordinaryCount = 0, zeroCount = 0, totalCount = 0, zramCount = 0;
        int ordinaryDisabled = 0, zeroDisabled = 0, totalDisabled = 0, zramDisabled = 0;
        int zeroDisabledWithValue = 0, zramDisabledWithValue = 0;
        int zramNormalDisabled = 0, zramWritebackDisabled = 0;
        long disabledZramValue = 0;
        Aggregate aOrdinary = aggregate(samples, bucket, 0), aZero = aggregate(samples, bucket, 1);
        Aggregate aTotal = aggregate(samples, bucket, 2), aZram = aggregate(samples, bucket, 3);
        Aggregate aZramNormal = aggregate(samples, bucket, 4), aZramWriteback = aggregate(samples, bucket, 5);
        ordinary = aOrdinary.value; ordinaryCount = aOrdinary.count;
        zero = aZero.value; zeroCount = aZero.count;
        subtotal = aTotal.value; totalCount = aTotal.count;
        zram = aZram.value; zramCount = aZram.count;
        for (HostMetrics sample : samples.values()) {
            if (sample.sampleTime / windowMillis != bucket) continue;
            if (sample.zeroDisabled) { zeroDisabled++; if (sample.zero != null && sample.zero == 0) zeroDisabledWithValue++; }
            if (sample.zramDisabled) {
                zramDisabled++;
                if (sample.zram != null && sample.zram == 0) { zramDisabledWithValue++; disabledZramValue += sample.zram; }
            }
            if (sample.zramSplitDisabled) { zramNormalDisabled++; zramWritebackDisabled++; }
        }
        boolean totalOverflow = false;
        for (HostMetrics sample : samples.values()) {
                if (sample.sampleTime / windowMillis != bucket) { continue; }
                if (sample.totalSeries) {
                    count++;
                    try { total = Math.addExact(total, sample.saved); } catch (ArithmeticException e) { totalOverflow = true; }
                }
        }
        zramNormal = aZramNormal.value; zramWriteback = aZramWriteback.value;
        if (totalOverflow) { total = 0; count = 0; }
        result.setCoveredHosts(count);
        if (ordinaryCount > 0 && !aOrdinary.overflow) result.setKsmOrdinaryBytes(ordinary);
        if (zeroCount > 0 && !aZero.overflow) result.setKsmZeroBytes(zero);
        if (totalCount > 0 && !aTotal.overflow) result.setKsmTotalBytes(subtotal);
        if (zramCount > 0 && !aZram.overflow) result.setZramBytes(zram);
        if (count > 0) {
            result.setSampleTime(bucket * windowMillis);
            result.setTotalSavedEstimateBytes(total);
            result.setQuality(count == expected.size() ? "Fresh" : "Partial");
        } else if (ordinaryCount > 0 || zeroCount > 0 || totalCount > 0 || zramCount > 0
                || zeroDisabledWithValue > 0 || zramDisabledWithValue > 0) {
            result.setSampleTime(bucket * windowMillis);
            result.setQuality("Partial");
        }
        if (zramCount == 0 && zramDisabledWithValue > 0 && !aZram.overflow) result.setZramBytes(disabledZramValue);
        if (zeroCount == 0 && zeroDisabledWithValue > 0 && !aZero.overflow) result.setKsmZeroBytes(0L);
        if ((aZramNormal.count > 0 || zramNormalDisabled > 0) && !aZramNormal.overflow) result.setZramNormalBytes(zramNormal);
        if ((aZramWriteback.count > 0 || zramWritebackDisabled > 0) && !aZramWriteback.overflow) result.setZramWritebackBytes(zramWriteback);
        Map<String, Map<String, Object>> coverage = new LinkedHashMap<>();
        coverage.put("ksmOrdinaryBytes", coverage(expected.size(), ordinaryCount, ordinaryCount, 0, aOrdinary.overflow));
        coverage.put("ksmZeroBytes", coverage(expected.size(), zeroCount + (aZero.overflow ? 0 : zeroDisabledWithValue),
                zeroCount, zeroDisabledWithValue, aZero.overflow));
        coverage.put("ksmTotalBytes", coverage(expected.size(), totalCount, totalCount, 0, aTotal.overflow));
        coverage.put("zramBytes", coverage(expected.size(), zramCount + (aZram.overflow ? 0 : zramDisabledWithValue),
                zramCount, zramDisabledWithValue, aZram.overflow));
        coverage.put("zramNormalBytes", coverage(expected.size(), aZramNormal.count + zramNormalDisabled,
                aZramNormal.count, zramNormalDisabled, aZramNormal.overflow));
        coverage.put("zramWritebackBytes", coverage(expected.size(), aZramWriteback.count + zramWritebackDisabled,
                aZramWriteback.count, zramWritebackDisabled, aZramWriteback.overflow));
        coverage.put("totalSavedEstimateBytes", coverage(expected.size(), count, count, 0, totalOverflow));
        result.setMetricCoverage(coverage);
        return result;
    }

    private static final class Aggregate { long value; int count; boolean overflow; }
    private static Aggregate aggregate(Map<String, HostMetrics> samples, long bucket, int metric) {
        Aggregate result = new Aggregate();
        for (HostMetrics sample : samples.values()) {
            if (sample.sampleTime / MemoryOptimizationGlobalConfig.summaryWindowMillis() != bucket) continue;
            Long value = metric == 0 ? sample.ordinary : metric == 1 ? sample.zero
                    : metric == 2 ? sample.total : metric == 3 ? sample.zram
                    : metric == 4 ? sample.zramNormal : sample.zramWriteback;
            boolean observed = metric == 0 ? sample.ordinaryObserved : metric == 1 ? sample.zeroObserved
                    : metric == 2 ? sample.totalObserved : metric == 3 ? sample.zramObserved : sample.zramSplitObserved;
            if (value == null || !observed || ((metric < 3 || metric == 4) && value < 0)) continue;
            if (!result.overflow) {
                try { result.value = Math.addExact(result.value, value); result.count++; }
                catch (ArithmeticException overflow) { result.value = 0; result.count = 0; result.overflow = true; }
            }
        }
        return result;
    }

    private static Map<String, Object> coverage(int expected, int covered, int observed, int disabled, boolean overflow) {
        Map<String, Object> value = new LinkedHashMap<>(); value.put("expectedHosts", expected);
        value.put("coveredHosts", covered);
        String quality = overflow ? "Unknown" : covered == 0 ? "Missing"
                : covered < expected ? "Partial" : observed == 0 && disabled > 0 ? "Disabled" : "Fresh";
        value.put("quality", quality);
        return value;
    }

    private static final class HostMetrics {
        long sampleTime, saved, bucket;
        Long ordinary, zero, total, zram, zramNormal, zramWriteback;
        boolean ordinaryObserved, zeroObserved, totalObserved, zramObserved, zramSplitObserved;
        boolean totalSeries, metricQualityPresent;
        boolean zeroDisabled, zramDisabled, zramSplitDisabled;
    }

    private static HostMetrics parseMetrics(JsonObject s, long now) {
        try {
            if (!"mechanism-estimate-v1".equals(string(s, "formulaVersion"))
                    || !s.has("sampleTime") || !s.get("sampleTime").isJsonPrimitive()
                    || !s.has("quality") || !("Fresh".equals(s.get("quality").getAsString())
                    || "Partial".equals(s.get("quality").getAsString()))) return null;
            HostMetrics m = new HostMetrics(); Long timestamp = number(s, "sampleTime");
            if (timestamp == null) return null;
            m.sampleTime = timestamp;
            if (m.sampleTime > now + 5000 || now - m.sampleTime >= MemoryOptimizationGlobalConfig.displayTtlMillis()) return null;
            JsonObject q = object(s, "metricQuality");
            m.ordinary = number(s, "ksmOrdinaryBytes"); m.zero = number(s, "ksmZeroBytes");
            m.total = number(s, "ksmTotalBytes"); m.zram = number(s, "zramBytes");
            m.zramNormal = number(s, "zramNormalBytes"); m.zramWriteback = number(s, "zramWritebackBytes");
            m.metricQualityPresent = s.has("metricQuality");
            m.ordinaryObserved = usable(q, "ksm_ordinary", m.ordinary, m.metricQualityPresent);
            m.zeroObserved = usable(q, "ksm_zero", m.zero, m.metricQualityPresent);
            m.totalObserved = usable(q, "ksm_total", m.total, m.metricQualityPresent);
            m.zramObserved = usable(q, "zram", m.zram, m.metricQualityPresent);
            m.zeroDisabled = "disabled".equalsIgnoreCase(quality(q, "ksm_zero"));
            m.zramDisabled = "disabled".equalsIgnoreCase(quality(q, "zram"));
            m.totalSeries = "mechanism-estimate-v1".equals(string(s, "formulaVersion"))
                    && "Fresh".equals(string(s, "quality"))
                    && seriesUsable(q, "ksm_ordinary", m.ordinary, m.metricQualityPresent)
                    && seriesUsable(q, "ksm_zero", m.zero, m.metricQualityPresent)
                    && seriesUsable(q, "ksm_total", m.total, m.metricQualityPresent)
                    && seriesUsable(q, "zram", m.zram, m.metricQualityPresent)
                    && m.ordinary >= 0 && m.zero >= 0 && m.total >= 0;
            if (m.totalSeries) {
                try {
                    if (Math.addExact(m.ordinary, m.zero) != m.total || s.get("totalSavedEstimateBytes") == null
                            || s.get("zramBytes") == null) m.totalSeries = false;
                    else { Long saved = number(s, "totalSavedEstimateBytes"); if (saved == null || Math.addExact(m.total, m.zram) != saved) m.totalSeries = false; else m.saved = saved; }
                } catch (ArithmeticException overflow) { m.totalSeries = false; }
            }
            try {
                boolean splitMatches = m.zramNormal != null && m.zramWriteback != null && m.zram != null
                        && Math.addExact(m.zramNormal, m.zramWriteback) == m.zram;
                m.zramSplitObserved = splitMatches
                        && (!m.metricQualityPresent || "observed".equalsIgnoreCase(quality(q, "zram")));
                m.zramSplitDisabled = splitMatches && m.zramNormal == 0 && m.zramWriteback == 0 && m.zram == 0
                        && "disabled".equalsIgnoreCase(quality(q, "zram"));
            } catch (ArithmeticException overflow) {
                m.zramSplitObserved = false;
                m.zramSplitDisabled = false;
            }
            return m;
        } catch (RuntimeException e) { return null; }
    }
    private static Long number(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive() || !o.getAsJsonPrimitive(key).isNumber()) return null;
        try {
            BigDecimal decimal = new BigDecimal(o.get(key).getAsString());
            return decimal.stripTrailingZeros().scale() <= 0 ? decimal.longValueExact() : null;
        } catch (RuntimeException e) { return null; }
    }
    private static String quality(JsonObject q, String key) { return q == null ? null : string(q, key); }
    private static boolean usable(JsonObject q, String key, Long value, boolean qualityPresent) {
        String quality = quality(q, key);
        return value != null && (!qualityPresent || "observed".equalsIgnoreCase(quality));
    }
    private static boolean seriesUsable(JsonObject q, String key, Long value, boolean qualityPresent) {
        String quality = quality(q, key);
        return value != null && (!qualityPresent || "observed".equalsIgnoreCase(quality)
                || ("disabled".equalsIgnoreCase(quality) && value == 0));
    }

    @SuppressWarnings("unchecked")
    private static void classifyCurrentStatus(MemoryStateVO host, long now, Map<String, Integer> counts) {
        if (host == null || host.getState() == null || host.getLastSampleTime() == null) {
            increment(counts, "unknown"); return;
        }
        long sample = host.getLastSampleTime();
        if (sample > now + 5000) { increment(counts, "unknown"); return; }
        if (now - sample >= MemoryOptimizationGlobalConfig.displayTtlMillis()) {
            // Expired data is evidence of staleness only. It must not be used
            // to claim that optimization is currently enabled or disabled.
            increment(counts, "stale"); return;
        }
        try {
            JsonObject state = new JsonParser().parse(host.getState()).getAsJsonObject();
            JsonObject caps = object(state, "capabilities");
            JsonObject actual = object(state, "actual");
            JsonObject savings = object(state, "savings");
            boolean classified = false;
            String phase = string(state, "phase");
            JsonObject lifecycle = object(state, "lifecycle");
            if ("PAUSED".equalsIgnoreCase(phase)
                    || (lifecycle != null && bool(lifecycle, "paused"))) {
                increment(counts, "paused"); classified = true;
            }
            if (Boolean.TRUE.equals(boolObject(state, "inventoryComplete"))
                    && state.has("vms") && state.get("vms").isJsonObject()
                    && state.getAsJsonObject("vms").entrySet().isEmpty()) {
                increment(counts, "noRunningVms"); classified = true;
            }
            String reason = string(caps, "zramReasonCode");
            if (reason == null) { reason = string(caps, "reasonCode"); }
            boolean zramUnavailable = Boolean.FALSE.equals(boolObject(caps, "zram"));
            if (Boolean.FALSE.equals(boolObject(caps, "supported"))
                    || (zramUnavailable && reason != null && reason.startsWith("UNSUPPORTED_"))) {
                increment(counts, "unsupported"); classified = true;
            }
            JsonObject hostFeedback = object(state, "host");
            boolean explicitConditions = Boolean.FALSE.equals(boolObject(hostFeedback, "safe"))
                    || "CONDITIONS_UNMET".equals(reason)
                    || "HOST_FEEDBACK_UNSAFE".equals(string(hostFeedback, "reason"))
                    || "HOST_FEEDBACK_UNSAFE".equals(reason);
            if (explicitConditions) {
                increment(counts, "conditionsUnmet"); classified = true;
            }
            if (Boolean.TRUE.equals(boolObject(caps, "rebootRequired"))) {
                increment(counts, "rebootRequired"); classified = true;
            }
            Boolean ksmEnabled = boolObject(object(actual, "ksm"), "enabled");
            Boolean zramEnabled = boolObject(object(actual, "zram"), "efficiencyEnabled");
            boolean enabled = Boolean.TRUE.equals(ksmEnabled) || Boolean.TRUE.equals(zramEnabled);
            if (Boolean.FALSE.equals(ksmEnabled) && Boolean.FALSE.equals(zramEnabled)) {
                increment(counts, "disabled"); classified = true;
            }
            Boolean zramEnabledState = boolObject(object(actual, "zram"), "efficiencyEnabled");
            String zramReason = string(caps, "zramReasonCode");
            boolean zramOptional = Boolean.FALSE.equals(boolObject(caps, "zram"))
                    && !Boolean.TRUE.equals(zramEnabledState)
                    && zramReason != null && (zramReason.startsWith("UNSUPPORTED_") || "ABI_INCOMPLETE".equals(zramReason));
            if (hasBlockingErrors(actual, zramOptional, false, true) || "error".equalsIgnoreCase(string(savings, "quality"))) {
                increment(counts, "error"); classified = true;
            }
            if (enabled && "Fresh".equals(string(savings, "quality"))
                    && savings.has("sampleTime") && !savings.get("sampleTime").isJsonNull()
                    && savings.get("sampleTime").getAsLong() <= now + 5000
                    && now - savings.get("sampleTime").getAsLong() < MemoryOptimizationGlobalConfig.displayTtlMillis()
                    && savings.has("totalSavedEstimateBytes")
                    && savings.get("totalSavedEstimateBytes").isJsonPrimitive()) {
                long saved = savings.get("totalSavedEstimateBytes").getAsLong();
                increment(counts, saved <= 0L ? "enabledNoSavings" : "active"); classified = true;
            }
            if (!classified) { increment(counts, "unknown"); }
        } catch (RuntimeException e) { increment(counts, "unknown"); }
    }

    private static void increment(Map<String, Integer> counts, String key) {
        counts.put(key, counts.containsKey(key) ? counts.get(key) + 1 : 1);
    }
    private static JsonObject object(JsonObject parent, String key) {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject()
                ? parent.getAsJsonObject(key) : null;
    }
    private static String string(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : null;
    }
    private static Boolean boolObject(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isBoolean()
                ? object.get(key).getAsBoolean() : null;
    }
    private static boolean bool(JsonObject object, String key) {
        return Boolean.TRUE.equals(boolObject(object, key));
    }
    private static boolean hasErrors(JsonObject object) {
        if (object == null) { return false; }
        if ("unknown".equalsIgnoreCase(string(object, "quality")) && string(object, "reason") != null) {
            return true; // A native actual-value read explicitly failed.
        }
        for (Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet()) {
            if ("errors".equals(entry.getKey()) && entry.getValue().isJsonArray()
                    && entry.getValue().getAsJsonArray().size() > 0) { return true; }
            if (entry.getValue().isJsonObject() && hasErrors(entry.getValue().getAsJsonObject())) { return true; }
        }
        return false;
    }

    private static boolean hasBlockingErrors(JsonObject object, boolean optionalZram, boolean optionalAllowed, boolean rootActual) {
        if (object == null) return false;
        if ("unknown".equalsIgnoreCase(string(object, "quality")) && string(object, "reason") != null) {
            String reason = string(object, "reason").toLowerCase(Locale.ROOT);
            if (!(optionalAllowed && optionalZram && reason.contains("[errno 2] no such file or directory"))) return true;
        }
        for (Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet()) {
            if ("errors".equals(entry.getKey()) && entry.getValue().isJsonArray()) {
                for (com.google.gson.JsonElement error : entry.getValue().getAsJsonArray()) {
                    String text = error.toString().toLowerCase(Locale.ROOT);
                    if (!(optionalAllowed && optionalZram && text.contains("[errno 2] no such file or directory"))) return true;
                }
            } else if (entry.getValue().isJsonObject()
                    && hasBlockingErrors(entry.getValue().getAsJsonObject(), optionalZram,
                    rootActual && optionalZram && ("zram".equals(entry.getKey()) || "writeback".equals(entry.getKey()))
                    && !Boolean.TRUE.equals(boolObject(entry.getValue().getAsJsonObject(), "enabled"))
                    && !Boolean.TRUE.equals(boolObject(entry.getValue().getAsJsonObject(), "efficiencyEnabled")), false)) return true;
        }
        return false;
    }
}
