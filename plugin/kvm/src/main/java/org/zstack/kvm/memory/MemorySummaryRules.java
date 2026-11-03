package org.zstack.kvm.memory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/** Aggregate complete same-window Host samples; never join independent metric peaks. */
public final class MemorySummaryRules {
    private static final CLogger logger = Utils.getLogger(MemorySummaryRules.class);
    private MemorySummaryRules() { }
    /** Per-Host savings use the Host state projection; a Host without state stays Unknown. */
    public static List<MemoryHostSavingsInventory> hostDetails(List<String> hosts, List<MemoryStateVO> states,
            Map<String, Map<String, Object>> samples, long now) {
        Map<String, MemoryStateVO> byHost = new HashMap<>();
        for (MemoryStateVO state : states) { byHost.put(state.getHostUuid(), state); }
        List<MemoryHostSavingsInventory> details = new java.util.ArrayList<>();
        for (String host : new java.util.LinkedHashSet<>(hosts)) {
            MemoryHostSavingsInventory detail = new MemoryHostSavingsInventory();
            detail.setHostUuid(host);
            MemoryStateVO state = byHost.get(host);
            if (state == null) {
                detail.setMetrics(new LinkedHashMap<>()); detail.setQuality("Unknown");
            } else {
                Map<String, Object> savings = samples.get(host);
                detail.setMetrics(MemoryMetricsPresentation.metrics(savings, now));
                detail.setQuality(MemoryMetricsPresentation.quality(savings, now));
                Object sampled = detail.getMetrics().get("sampleTime");
                detail.setSampleTime(sampled instanceof Number ? ((Number) sampled).longValue() : null);
            }
            details.add(detail);
        }
        return details;
    }

    public static MemorySummaryInventory summarize(List<String> hosts, List<MemoryStateVO> states, long now) {
        Map<String, String> unreadable = new LinkedHashMap<>();
        MemorySummaryInventory summary = summarize(hosts, states, now, unreadable);
        if (!unreadable.isEmpty()) {
            logger.error(String.format("%d Host memory sample(s) could not be read for the summary and count as missing: %s",
                    unreadable.size(), unreadable));
        }
        return summary;
    }

    /** As summarize, naming each Host whose sample could not be read in unreadable. */
    static MemorySummaryInventory summarize(List<String> hosts, List<MemoryStateVO> states, long now, Map<String, String> unreadable) {
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
            JsonObject json = storedObject(host.getState());
            if (json == null) {
                // A sample that cannot be read stays missing; the Host is named once for the summary's error.
                unreadable.putIfAbsent(host.getHostUuid(), UNREADABLE_SAMPLE);
                continue;
            }
            JsonObject savings = object(json, "savings");
            // A POJO default must never turn an absent producer schema into a supported one.
            if (savings == null || !savings.has("formulaVersion") || !savings.has("quality")) { continue; }
            HostMetrics sample = parseMetrics(savings, now);
            if (sample == null) { continue; }
            bucket = Math.max(bucket, sample.sampleTime / windowMillis);
            samples.put(host.getHostUuid(), sample);
        }
        MemorySummaryInventory result = new MemorySummaryInventory();
        result.setExpectedHosts(expected.size()); result.setQuality("Unknown");
        for (String host : expected) {
            if (!byHost.containsKey(host)) { increment(currentStatus, "unknown"); }
            else { classifyCurrentStatus(byHost.get(host), now, currentStatus, unreadable); }
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
        Aggregate aSaved = new Aggregate();
        for (HostMetrics sample : samples.values()) {
            if (sample.sampleTime / windowMillis == bucket && sample.totalSeries) { aSaved.add(sample.saved); }
        }
        zramNormal = aZramNormal.value; zramWriteback = aZramWriteback.value;
        total = aSaved.value; count = aSaved.count;
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
        coverage.put("ksmOrdinaryBytes", coverage(expected.size(), ordinaryCount, ordinaryCount, 0, aOrdinary));
        coverage.put("ksmZeroBytes", coverage(expected.size(), zeroCount + (aZero.overflow ? 0 : zeroDisabledWithValue),
                zeroCount, zeroDisabledWithValue, aZero));
        coverage.put("ksmTotalBytes", coverage(expected.size(), totalCount, totalCount, 0, aTotal));
        coverage.put("zramBytes", coverage(expected.size(), zramCount + (aZram.overflow ? 0 : zramDisabledWithValue),
                zramCount, zramDisabledWithValue, aZram));
        coverage.put("zramNormalBytes", coverage(expected.size(), aZramNormal.count + zramNormalDisabled,
                aZramNormal.count, zramNormalDisabled, aZramNormal));
        coverage.put("zramWritebackBytes", coverage(expected.size(), aZramWriteback.count + zramWritebackDisabled,
                aZramWriteback.count, zramWritebackDisabled, aZramWriteback));
        coverage.put("totalSavedEstimateBytes", coverage(expected.size(), count, count, 0, aSaved));
        result.setMetricCoverage(coverage);
        return result;
    }

    private static final class Aggregate {
        long value; int count; boolean overflow;

        /** A sum that overflows is unknown: it reports no value and no covered Host. */
        void add(long next) {
            if (overflow) { return; }
            Long sum = addOrNull(value, next);
            if (sum == null) { value = 0; count = 0; overflow = true; } else { value = sum; count++; }
        }
    }

    /** a + b, or null when the sum overflows a long. */
    static Long addOrNull(long a, long b) {
        long sum = a + b;
        return ((a ^ sum) & (b ^ sum)) < 0 ? null : sum;
    }
    private static Aggregate aggregate(Map<String, HostMetrics> samples, long bucket, int metric) {
        Aggregate result = new Aggregate();
        for (HostMetrics sample : samples.values()) {
            if (sample.sampleTime / MemoryOptimizationGlobalConfig.summaryWindowMillis() != bucket) continue;
            Long value = metric == 0 ? sample.ordinary : metric == 1 ? sample.zero
                    : metric == 2 ? sample.total : metric == 3 ? sample.zram
                    : metric == 4 ? sample.zramNormal : sample.zramWriteback;
            boolean observed = metric == 0 ? sample.ordinaryObserved : metric == 1 ? sample.zeroObserved
                    : metric == 2 ? sample.totalObserved : metric == 3 ? sample.zramObserved : sample.zramSplitObserved;
            // KSM counters cannot be negative. ZRAM estimates (including
            // resident savings) can be negative when allocation exceeds data.
            if (value == null || !observed || (metric < 3 && value < 0)) continue;
            result.add(value);
        }
        return result;
    }

    private static Map<String, Object> coverage(int expected, int covered, int observed, int disabled, Aggregate sum) {
        Map<String, Object> value = new LinkedHashMap<>(); value.put("expectedHosts", expected);
        value.put("coveredHosts", covered);
        String quality = sum.overflow ? "Unknown" : covered == 0 ? "Missing"
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
        m.ordinaryObserved = usable(s, "ksm_ordinary", m.ordinary);
        m.zeroObserved = usable(s, "ksm_zero", m.zero);
        m.totalObserved = usable(s, "ksm_total", m.total);
        m.zramObserved = usable(s, "zram", m.zram);
        m.zeroDisabled = "disabled".equalsIgnoreCase(quality(q, "ksm_zero"));
        m.zramDisabled = "disabled".equalsIgnoreCase(quality(q, "zram"));
        m.totalSeries = "mechanism-estimate-v1".equals(string(s, "formulaVersion"))
                && "Fresh".equals(string(s, "quality"))
                && seriesUsable(s, "ksm_ordinary", m.ordinary)
                && seriesUsable(s, "ksm_zero", m.zero)
                && seriesUsable(s, "ksm_total", m.total)
                && seriesUsable(s, "zram", m.zram)
                && m.ordinary >= 0 && m.zero >= 0 && m.total >= 0;
        if (m.totalSeries) {
            Long ksm = addOrNull(m.ordinary, m.zero);
            Long saved = number(s, "totalSavedEstimateBytes");
            Long expected = addOrNull(m.total, m.zram);
            if (ksm == null || !ksm.equals(m.total) || s.get("totalSavedEstimateBytes") == null || s.get("zramBytes") == null
                    || saved == null || !saved.equals(expected)) { m.totalSeries = false; } else { m.saved = saved; }
        }
        Long split = m.zramNormal == null || m.zramWriteback == null ? null : addOrNull(m.zramNormal, m.zramWriteback);
        boolean splitMatches = split != null && split.equals(m.zram);
        m.zramSplitObserved = splitMatches
                && (!m.metricQualityPresent || "observed".equalsIgnoreCase(quality(q, "zram")));
        m.zramSplitDisabled = splitMatches && m.zramNormal == 0 && m.zramWriteback == 0 && m.zram == 0
                && "disabled".equalsIgnoreCase(quality(q, "zram"));
        return m;
    }
    private static Long number(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive() || !o.getAsJsonPrimitive(key).isNumber()) return null;
        return MemoryObservationRules.parseExactLong(o.get(key).getAsString());
    }

    private static final String UNREADABLE_SAMPLE = "the stored sample is not a JSON object";

    /** A value present but not an exact number makes the whole sample unreadable. */
    private static boolean malformedNumber(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull() && number(o, key) == null;
    }

    /** The JSON object a stored sample holds, or null when it cannot be read as one. */
    private static JsonObject storedObject(String json) {
        JsonElement element = MemoryObservationRules.readJson(json, JsonElement.class, "stored memory sample is unreadable");
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
    private static String quality(JsonObject q, String key) { return q == null ? null : string(q, key); }
    /** A value is usable without a metricQuality, or with its quality observed; a metricQuality that is not an object observes nothing. */
    private static boolean usable(JsonObject sample, String key, Long value) {
        String quality = quality(object(sample, "metricQuality"), key);
        return value != null && (!sample.has("metricQuality") || "observed".equalsIgnoreCase(quality));
    }
    private static boolean seriesUsable(JsonObject sample, String key, Long value) {
        String quality = quality(object(sample, "metricQuality"), key);
        return value != null && (!sample.has("metricQuality") || "observed".equalsIgnoreCase(quality)
                || ("disabled".equalsIgnoreCase(quality) && value == 0));
    }

    @SuppressWarnings("unchecked")
    private static void classifyCurrentStatus(MemoryStateVO host, long now, Map<String, Integer> counts,
                                              Map<String, String> unreadable) {
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
        JsonObject state = storedObject(host.getState());
        JsonObject stored = object(state, "savings");
        if (state == null || malformedNumber(stored, "sampleTime") || malformedNumber(stored, "totalSavedEstimateBytes")) {
            // A sample that cannot be read is counted once, as unknown, and named for the summary's error.
            unreadable.putIfAbsent(host.getHostUuid(), state == null ? UNREADABLE_SAMPLE : "a saving of the stored sample is not an exact number");
            increment(counts, "unknown");
            return;
        }
        Map<String, Integer> hostCounts = new HashMap<>();
        JsonObject caps = object(state, "capabilities");
        JsonObject ksm = object(state, "ksm");
        JsonObject zram = object(state, "zram");
        JsonObject savings = object(state, "savings");
        boolean classified = false;
        String control = string(state, "control");
        if ("paused".equals(control) || "drained".equals(control)) {
            increment(hostCounts, "paused"); classified = true;
        }
        if (Boolean.TRUE.equals(boolObject(state, "inventoryComplete"))
                && state.has("vms") && state.get("vms").isJsonObject()
                && state.getAsJsonObject("vms").entrySet().isEmpty()) {
            increment(hostCounts, "noRunningVms"); classified = true;
        }
        String reason = string(caps, "zramReasonCode");
        if (reason == null) { reason = string(caps, "reasonCode"); }
        boolean zramUnavailable = Boolean.FALSE.equals(boolObject(caps, "zram"));
        if (Boolean.FALSE.equals(boolObject(caps, "supported"))
                || (zramUnavailable && reason != null && reason.startsWith("UNSUPPORTED_"))) {
            increment(hostCounts, "unsupported"); classified = true;
        }
        JsonObject hostFeedback = object(object(zram, "status"), "host");
        boolean explicitConditions = Boolean.FALSE.equals(boolObject(hostFeedback, "safe"))
                || "CONDITIONS_UNMET".equals(reason)
                || "HOST_FEEDBACK_UNSAFE".equals(string(hostFeedback, "reason"))
                || "HOST_FEEDBACK_UNSAFE".equals(reason);
        if (explicitConditions) {
            increment(hostCounts, "conditionsUnmet"); classified = true;
        }
        if (Boolean.TRUE.equals(boolObject(caps, "rebootRequired"))) {
            increment(hostCounts, "rebootRequired"); classified = true;
        }
        Boolean ksmEnabled = boolObject(ksm, "enabled");
        Boolean zramEnabled = boolObject(zram, "efficiencyEnabled");
        boolean enabled = Boolean.TRUE.equals(ksmEnabled) || Boolean.TRUE.equals(zramEnabled);
        if (Boolean.FALSE.equals(ksmEnabled) && Boolean.FALSE.equals(zramEnabled)) {
            increment(hostCounts, "disabled"); classified = true;
        }
        // An observation the Agent could not read reports its reason.
        boolean zramObserved = !"absent".equals(string(zram, "serviceState"));
        if (string(ksm, "reason") != null || zramObserved && string(zram, "reason") != null
                || "error".equalsIgnoreCase(string(savings, "quality"))) {
            increment(hostCounts, "error"); classified = true;
        }
        Long sampleTime = number(savings, "sampleTime");
        Long saved = number(savings, "totalSavedEstimateBytes");
        if (enabled && "Fresh".equals(string(savings, "quality")) && sampleTime != null && saved != null
                && sampleTime <= now + 5000 && now - sampleTime < MemoryOptimizationGlobalConfig.displayTtlMillis()) {
            increment(hostCounts, saved <= 0L ? "enabledNoSavings" : "active"); classified = true;
        }
        if (!classified) { increment(hostCounts, "unknown"); }
        // Counted only once the whole sample is classified.
        hostCounts.forEach((key, value) -> counts.merge(key, value, Integer::sum));
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
}
