package org.zstack.kvm.memory;

import org.junit.Test;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.*;
import static org.junit.Assert.*;

public class MemorySummaryRulesTest {
    private MemoryStateVO current(String host, long sampledAt, String state) {
        MemoryStateVO value = new MemoryStateVO(); value.setHostUuid(host);
        value.setLastSampleTime(sampledAt); value.setState(state); return value;
    }
    private Map<String, Integer> reasons(MemoryStateVO... rows) {
        return MemorySummaryRules.summarize(Collections.singletonList("a"), Arrays.asList(rows), now).getCurrentStatus();
    }
    @Test public void currentReasonsUseActualSwitchesNotZeroOrDesiredConfiguration() {
        String zero = "\"savings\":{\"quality\":\"Fresh\",\"sampleTime\":300000,\"totalSavedEstimateBytes\":0}";
        Map<String, Integer> unknown = reasons(current("a", now, "{" + zero + "}"));
        assertEquals(Integer.valueOf(1), unknown.get("unknown"));
        assertFalse(unknown.containsKey("enabledNoSavings"));
        String off = "{\"actual\":{\"ksm\":{\"enabled\":false},\"zram\":{\"efficiencyEnabled\":false}}," + zero + "}";
        assertEquals(Integer.valueOf(1), reasons(current("a", now, off)).get("disabled"));
        assertFalse(reasons(current("a", now, off)).containsKey("enabledNoSavings"));
        String on = off.replace("\"enabled\":false", "\"enabled\":true");
        assertEquals(Integer.valueOf(1), reasons(current("a", now, on)).get("enabledNoSavings"));
        String positive = on.replace("\"totalSavedEstimateBytes\":0", "\"totalSavedEstimateBytes\":120");
        Map<String, Integer> active = reasons(current("a", now, positive));
        assertEquals(Integer.valueOf(1), active.get("active"));
        assertFalse(active.containsKey("unknown"));
    }
    @Test public void expiredOrMissingSamplesDoNotClaimCurrentOffOrNoVms() {
        String off = "{\"actual\":{\"ksm\":{\"enabled\":false},\"zram\":{\"efficiencyEnabled\":false}},\"vms\":{},\"inventoryComplete\":true}";
        assertEquals(Collections.singletonMap("stale", 1), reasons(current("a", now - 90000, off)));
        assertEquals(Collections.singletonMap("noRunningVms", 1), reasons(current("a", now,
                "{\"vms\":{},\"inventoryComplete\":true}")));
        assertEquals(Collections.singletonMap("unknown", 1), reasons(current("a", now,
                "{\"vms\":{},\"inventoryComplete\":false}")));
        assertEquals(Collections.singletonMap("unknown", 1), reasons());
    }
    @Test public void statusCountsAreScopedDeduplicatedAndSeparateFromMetricTime() {
        MemoryStateVO a = current("a", now, "{\"capabilities\":{\"rebootRequired\":true,\"zram\":false,\"zramReasonCode\":\"REBOOT_REQUIRED\"}}");
        MemoryStateVO b = current("unrequested", now, "{\"capabilities\":{\"supported\":false}}");
        MemorySummaryInventory summary = MemorySummaryRules.summarize(Arrays.asList("a", "a"), Arrays.asList(a, a, b), now);
        assertEquals(Collections.singletonMap("rebootRequired", 1), summary.getCurrentStatus());
        assertEquals(Long.valueOf(now), summary.getCurrentStatusSampleTime());
        assertNull(summary.getSampleTime());
        assertNull(summary.getTotalSavedEstimateBytes());
    }
    @Test public void independentlySupportedKsmDoesNotHideZramLimitationsOrReadErrors() {
        String state = "{\"capabilities\":{\"supported\":true,\"ksm\":true,\"zram\":false,\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"},"
                + "\"actual\":{\"ksm\":{\"enabled\":true},\"zram\":{\"errors\":[{\"operation\":\"host-cpu-state\"}]}},"
                + "\"host\":{\"safe\":false},\"lifecycle\":{\"paused\":true}}";
        Map<String, Integer> counts = reasons(current("a", now, state));
        for (String reason : Arrays.asList("unsupported", "conditionsUnmet", "error", "paused")) {
            assertEquals(Integer.valueOf(1), counts.get(reason));
        }
        assertFalse(counts.containsKey("disabled"));
    }
    @Test public void capabilityUnknownIsNotPresentedAsConditionsUnmet() {
        String state = "{\"capabilities\":{\"supported\":true,\"ksm\":true,"
                + "\"zram\":false,\"zramReasonCode\":\"CAPABILITY_UNKNOWN\"}}";
        Map<String, Integer> counts = reasons(current("a", now, state));
        assertEquals(Integer.valueOf(1), counts.get("unknown"));
        assertFalse(counts.containsKey("conditionsUnmet"));
    }
    @Test public void observationQueriesBypassOnlyHostStatusGate() {
        for (String action : Arrays.asList("state", "capabilities", "preview", "accounting", "operations", "reconcile")) {
            assertTrue(action, MemoryOptimizationManager.readOnlyAgentQuery(action));
        }
        for (String action : Arrays.asList("apply", "migration-hold", "drain-start",
                "drain-status", "lifecycle-pause", "capacity", "host-cpu", "vm-policy",
                "vm-management", "vm-management-shard-stage", "vm-management-shard-commit",
                "vm-management-shard-cancel", "tuning-state", "capacity-state", "host-cpu-state",
                "vm-policy-state")) {
            assertFalse(action, MemoryOptimizationManager.readOnlyAgentQuery(action));
        }
    }
    @Test public void exactTtlBoundaryIsExcluded() {
        MemorySummaryInventory result = MemorySummaryRules.summarize(Collections.singletonList("a"),
                Collections.singletonList(sample("a", now - 90000, "")), now);
        assertNull(result.getTotalSavedEstimateBytes());
        assertEquals(0, result.getCoveredHosts());
    }
    private final long now = 300000;
    private MemoryStateVO sample(String host, long timestamp, String extra) {
        MemoryStateVO result = new MemoryStateVO(); result.setHostUuid(host);
        result.setState("{\"savings\":{\"formulaVersion\":\"mechanism-estimate-v1\",\"quality\":\"Fresh\","
                + "\"sampleTime\":" + timestamp + ",\"ksmOrdinaryBytes\":100,\"ksmZeroBytes\":20,"
                + "\"ksmTotalBytes\":120,\"zramBytes\":30,\"totalSavedEstimateBytes\":150" + extra + "}}");
        return result;
    }
    @Test public void completeSameWindowNeverAddsSubtotalTwice() {
        MemorySummaryInventory result = MemorySummaryRules.summarize(Arrays.asList("a", "b"),
                Arrays.asList(sample("a", now, ""), sample("b", now + 1000, "")), now);
        assertEquals(Long.valueOf(300), result.getTotalSavedEstimateBytes());
        assertEquals(Long.valueOf(240), result.getKsmTotalBytes());
        assertEquals(2, result.getCoveredHosts()); assertEquals("Fresh", result.getQuality());
    }
    @Test public void missingOrWrongFormulaAndIncompleteQualityAreNotCompleteSamples() {
        MemoryStateVO missing = sample("a", now, "");
        missing.setState(missing.getState().replace("\"formulaVersion\":\"mechanism-estimate-v1\",", ""));
        assertNull(MemorySummaryRules.summarize(Arrays.asList("a"), Arrays.asList(missing), now).getTotalSavedEstimateBytes());
        MemoryStateVO partial = sample("a", now, ""); partial.setState(partial.getState().replace("Fresh", "Partial"));
        assertNull(MemorySummaryRules.summarize(Arrays.asList("a"), Arrays.asList(partial), now).getTotalSavedEstimateBytes());
    }
    @Test public void staleAndOtherWindowsDoNotFabricateFullCoverage() {
        MemorySummaryInventory result = MemorySummaryRules.summarize(Arrays.asList("a", "b", "c"),
                Arrays.asList(sample("a", now, ""), sample("b", now - 30000, ""), sample("c", now - 100000, "")), now);
        assertEquals(1, result.getCoveredHosts()); assertEquals(3, result.getExpectedHosts());
        assertEquals("Partial", result.getQuality()); assertEquals(Long.valueOf(150), result.getTotalSavedEstimateBytes());
    }
    @Test public void duplicateHostsAreNotCountedTwiceAndNegativeEstimatesArePreserved() {
        MemoryStateVO negative = sample("a", now, "");
        negative.setState(negative.getState().replace("\"zramBytes\":30", "\"zramBytes\":-130")
                .replace("\"totalSavedEstimateBytes\":150", "\"totalSavedEstimateBytes\":-10"));
        MemorySummaryInventory result = MemorySummaryRules.summarize(Arrays.asList("a", "a"),
                Arrays.asList(negative, negative), now);
        assertEquals(Long.valueOf(-10), result.getTotalSavedEstimateBytes()); assertEquals(1, result.getCoveredHosts());
    }
    @Test public void zramSplitIsOptionalForLegacySamplesButMustBeNonOverlapping() {
        MemoryStateVO first = sample("a", now, ",\"zramNormalBytes\":90,\"zramWritebackBytes\":-60");
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Collections.singletonList("a"), Collections.singletonList(first), now);
        assertEquals(Long.valueOf(90), result.getZramNormalBytes());
        assertEquals(Long.valueOf(-60), result.getZramWritebackBytes());
        assertEquals(1, result.getMetricCoverage().get("zramNormalBytes").get("coveredHosts"));
        assertEquals("Fresh", result.getMetricCoverage().get("zramNormalBytes").get("quality"));
        assertEquals("Fresh", result.getMetricCoverage().get("zramWritebackBytes").get("quality"));

        MemoryStateVO invalid = sample("a", now, ",\"zramNormalBytes\":90,\"zramWritebackBytes\":-50");
        result = MemorySummaryRules.summarize(Collections.singletonList("a"),
                Collections.singletonList(invalid), now);
        assertEquals("Malformed optional split must not erase total savings coverage", 1, result.getCoveredHosts());
        assertEquals("Missing", result.getMetricCoverage().get("zramNormalBytes").get("quality"));
    }

    @Test public void splitSeriesCoverageIsIndependentAndDisabledRequiresExplicitZeroEvidence() {
        MemoryStateVO split = sample("a", now, ",\"zramNormalBytes\":90,\"zramWritebackBytes\":-60");
        MemorySummaryInventory result = MemorySummaryRules.summarize(Arrays.asList("a", "b"),
                Collections.singletonList(split), now);
        assertEquals(Long.valueOf(90), result.getZramNormalBytes());
        assertEquals(1, result.getMetricCoverage().get("zramNormalBytes").get("coveredHosts"));
        assertEquals("Partial", result.getMetricCoverage().get("zramNormalBytes").get("quality"));

        MemoryStateVO disabledWithoutValue = current("a", now,
                "{\"savings\":{\"formulaVersion\":\"mechanism-estimate-v1\","
                        + "\"quality\":\"Partial\",\"sampleTime\":300000,"
                        + "\"metricQuality\":{\"zram\":\"disabled\"}}}");
        result = MemorySummaryRules.summarize(Collections.singletonList("a"),
                Collections.singletonList(disabledWithoutValue), now);
        assertEquals("Missing", result.getMetricCoverage().get("zramBytes").get("quality"));
        assertEquals(0, result.getMetricCoverage().get("zramBytes").get("coveredHosts"));
        assertEquals("Missing", result.getMetricCoverage().get("zramNormalBytes").get("quality"));

        MemoryStateVO disabledZeroSplit = current("a", now,
                "{\"savings\":{\"formulaVersion\":\"mechanism-estimate-v1\","
                        + "\"quality\":\"Partial\",\"sampleTime\":300000,\"zramBytes\":0,"
                        + "\"zramNormalBytes\":0,\"zramWritebackBytes\":0,"
                        + "\"metricQuality\":{\"zram\":\"disabled\"}}}");
        result = MemorySummaryRules.summarize(Collections.singletonList("a"),
                Collections.singletonList(disabledZeroSplit), now);
        assertEquals("Disabled", result.getMetricCoverage().get("zramNormalBytes").get("quality"));
        assertEquals(1, result.getMetricCoverage().get("zramNormalBytes").get("coveredHosts"));
        assertEquals("Disabled", result.getMetricCoverage().get("zramWritebackBytes").get("quality"));
    }

    @Test public void partialHostContributesObservedOrdinaryKsmWithoutFabricatingZeroOrTotal() {
        MemoryStateVO partial = current("a", now,
                "{\"capabilities\":{\"ksm\":true,\"zram\":false,"
                + "\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"},"
                + "\"actual\":{\"ksm\":{\"enabled\":true}},"
                + "\"savings\":{\"formulaVersion\":\"mechanism-estimate-v1\","
                + "\"quality\":\"Partial\",\"sampleTime\":300000,"
                + "\"ksmOrdinaryBytes\":100}}");
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Arrays.asList("a", "b", "c"), Collections.singletonList(partial), now);
        assertEquals(Long.valueOf(100), result.getKsmOrdinaryBytes());
        assertNull("missing zero-page counter must remain null", result.getKsmZeroBytes());
        assertNull("incomplete components cannot produce a total", result.getTotalSavedEstimateBytes());
        assertEquals("Partial", result.getQuality());
        assertEquals("coveredHosts retains total-series coverage semantics", 0, result.getCoveredHosts());
        JsonObject coverage = new JsonParser().parse(new Gson().toJson(result)).getAsJsonObject()
                .getAsJsonObject("metricCoverage");
        assertNotNull(coverage);
        assertEquals(3, coverage.getAsJsonObject("ksmOrdinaryBytes").get("expectedHosts").getAsInt());
        assertEquals(1, coverage.getAsJsonObject("ksmOrdinaryBytes").get("coveredHosts").getAsInt());
        assertEquals("Partial", coverage.getAsJsonObject("ksmOrdinaryBytes").get("quality").getAsString());
        assertEquals("Missing", coverage.getAsJsonObject("ksmZeroBytes").get("quality").getAsString());
    }

    @Test public void disabledOptionalZramErrorsDoNotMakeHostError() {
        String state = "{\"capabilities\":{\"ksm\":true,\"zram\":false,"
                + "\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"},"
                + "\"actual\":{\"ksm\":{\"enabled\":true},"
                + "\"zram\":{\"errors\":[{\"operation\":\"host-cpu-state\","
                + "\"reason\":\"[Errno 2] No such file or directory\"}]}},"
                + "\"savings\":{\"quality\":\"Partial\",\"sampleTime\":300000}}";
        Map<String, Integer> counts = reasons(current("a", now, state));
        assertFalse("unsupported optional ZRAM must not poison Host status", counts.containsKey("error"));
        assertEquals(Integer.valueOf(1), counts.get("unsupported"));
    }

    @Test public void summaryJsonExposesIndependentMetricCoverage() {
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Collections.singletonList("a"), Collections.singletonList(current("a", now,
                        "{\"savings\":{\"formulaVersion\":\"mechanism-estimate-v1\","
                                + "\"quality\":\"Partial\",\"sampleTime\":300000,"
                                + "\"ksmOrdinaryBytes\":100}}")), now);
        String json = new Gson().toJson(result);
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        assertTrue("coverage must be reported per metric", root.has("metricCoverage"));
        assertEquals("Missing", root.getAsJsonObject("metricCoverage")
                .getAsJsonObject("ksmZeroBytes").get("quality").getAsString());
        assertEquals("Missing", root.getAsJsonObject("metricCoverage")
                .getAsJsonObject("zramBytes").get("quality").getAsString());
    }

    @Test public void explicitDisabledZramZeroHasItsOwnDisabledCoverage() {
        MemoryStateVO state = current("a", now,
                "{\"savings\":{\"formulaVersion\":\"mechanism-estimate-v1\","
                        + "\"quality\":\"Partial\",\"sampleTime\":300000,"
                        + "\"ksmOrdinaryBytes\":100,\"zramBytes\":0,"
                        + "\"metricQuality\":{\"ksm_ordinary\":\"observed\","
                        + "\"ksm_zero\":\"missing\",\"zram\":\"disabled\"}}}");
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Collections.singletonList("a"), Collections.singletonList(state), now);
        JsonObject coverage = new JsonParser().parse(new Gson().toJson(result)).getAsJsonObject()
                .getAsJsonObject("metricCoverage");
        assertEquals("Disabled", coverage.getAsJsonObject("zramBytes").get("quality").getAsString());
        assertEquals(1, coverage.getAsJsonObject("zramBytes").get("coveredHosts").getAsInt());
    }

    @Test public void metricQualityMissingOrUnknownDoesNotExpandObservedCoverage() {
        MemoryStateVO state = current("a", now,
                "{\"savings\":{\"formulaVersion\":\"mechanism-estimate-v1\","
                        + "\"quality\":\"Partial\",\"sampleTime\":300000,"
                        + "\"ksmOrdinaryBytes\":100,\"metricQuality\":{"
                        + "\"ksm_ordinary\":\"missing\",\"ksm_zero\":\"unknown\"}}}");
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Collections.singletonList("a"), Collections.singletonList(state), now);
        JsonObject coverage = new JsonParser().parse(new Gson().toJson(result)).getAsJsonObject()
                .getAsJsonObject("metricCoverage");
        assertEquals(0, coverage.getAsJsonObject("ksmOrdinaryBytes").get("coveredHosts").getAsInt());
        assertEquals("Missing", coverage.getAsJsonObject("ksmOrdinaryBytes").get("quality").getAsString());
    }

    @Test public void malformedOptionalCountersDoNotEraseObservedOrdinaryKsm() {
        MemoryStateVO observed = current("a", now,
                "{\"savings\":{\"formulaVersion\":\"mechanism-estimate-v1\","
                        + "\"quality\":\"Partial\",\"sampleTime\":300000,"
                        + "\"ksmOrdinaryBytes\":100,\"ksmZeroBytes\":\"bad\","
                        + "\"ksmTotalBytes\":\"bad\",\"zramBytes\":\"bad\"}}");
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Collections.singletonList("a"), Collections.singletonList(observed), now);
        assertEquals(Long.valueOf(100), result.getKsmOrdinaryBytes());
        assertNull(result.getKsmZeroBytes()); assertNull(result.getKsmTotalBytes());
    }

    @Test public void futureAndBoundarySamplesCannotExpandPerMetricCoverage() {
        MemoryStateVO valid = current("a", now,
                "{\"savings\":{\"formulaVersion\":\"mechanism-estimate-v1\","
                        + "\"quality\":\"Partial\",\"sampleTime\":300000,"
                        + "\"ksmOrdinaryBytes\":100}}");
        MemoryStateVO future = current("b", now + 6000,
                "{\"savings\":{\"formulaVersion\":\"mechanism-estimate-v1\","
                        + "\"quality\":\"Partial\",\"sampleTime\":306000,"
                        + "\"ksmOrdinaryBytes\":200}}");
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Arrays.asList("a", "b"), Arrays.asList(valid, future), now);
        assertEquals(Long.valueOf(100), result.getKsmOrdinaryBytes());
        assertNull(result.getKsmZeroBytes()); assertEquals(0, result.getCoveredHosts());
    }

    @Test public void realKylinMixedObservationKeepsOrdinaryAndDoesNotReportOptionalZramError() throws Exception {
        JsonObject root = MemoryKylinFixture.nativeOff();
        JsonObject inventory = null;
        for (com.google.gson.JsonElement item : root.getAsJsonObject("body").getAsJsonArray("inventories")) {
            JsonObject candidate = item.getAsJsonObject();
            if ("ae99e7222906497281fb3d4141f78833".equals(candidate.get("hostUuid").getAsString())) {
                inventory = candidate; break;
            }
        }
        assertNotNull(inventory);
        JsonObject rawState = new JsonParser().parse(inventory.get("state").getAsString()).getAsJsonObject();
        JsonObject metricQuality = rawState.getAsJsonObject("savings").getAsJsonObject("metricQuality");
        assertEquals("observed", metricQuality.get("ksm_ordinary").getAsString());
        assertEquals("missing", metricQuality.get("ksm_zero").getAsString());
        assertEquals("disabled", metricQuality.get("zram").getAsString());
        MemoryStateVO row = new MemoryStateVO();
        row.setHostUuid(inventory.get("hostUuid").getAsString());
        row.setLastSampleTime(inventory.get("lastSampleTime").getAsLong());
        row.setState(inventory.get("state").getAsString());
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Collections.singletonList(row.getHostUuid()), Collections.singletonList(row),
                inventory.getAsJsonObject("metrics").get("presentationTime").getAsLong());
        assertEquals(Long.valueOf(163635200), result.getKsmOrdinaryBytes());
        assertNull(result.getKsmZeroBytes()); assertNull(result.getTotalSavedEstimateBytes());
        assertFalse(result.getCurrentStatus().containsKey("error"));
    }
}
