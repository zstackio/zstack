package org.zstack.kvm.memory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Independent arbiter RED tests for boundary contracts not covered by the
 * focused GREEN set.  These tests deliberately live outside the frozen RED
 * files and must not be weakened to match the candidate implementation.
 */
public class MemoryArbiterBoundaryTest {
    private static final long NOW = 300000L;

    private MemoryStateVO state(String host, String savings, String prefix) {
        MemoryStateVO value = new MemoryStateVO();
        value.setHostUuid(host);
        value.setLastSampleTime(NOW);
        String json = prefix + "\"savings\":{" + savings + "}}";
        // Fail at fixture construction instead of hiding malformed input in
        // the production parser's intentional Unknown fallback.
        assertTrue(JsonParser.parseString(json).isJsonObject());
        value.setState(json);
        return value;
    }

    private String partial(String fields) {
        return "\"formulaVersion\":\"mechanism-estimate-v1\",\"quality\":\"Partial\","
                + "\"sampleTime\":" + NOW + (fields.isEmpty() ? "" : "," + fields);
    }

    @Test public void fractionalCounterIsNotTruncatedIntoObservedBytes() {
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Collections.singletonList("a"),
                Collections.singletonList(state("a", partial("\"ksmOrdinaryBytes\":1.5"), "{")), NOW);
        assertNull("non-integer counters must remain missing, not truncate to 1", result.getKsmOrdinaryBytes());
        assertEquals(0, coverage(result, "ksmOrdinaryBytes").get("coveredHosts").getAsInt());
    }

    @Test public void unknownFormulaDoesNotMakeOrdinaryCounterTrustworthy() {
        String savings = "\"formulaVersion\":\"future-formula\",\"quality\":\"Partial\","
                + "\"sampleTime\":" + NOW + ",\"ksmOrdinaryBytes\":100";
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Collections.singletonList("a"),
                Collections.singletonList(state("a", savings, "{")), NOW);
        assertNull("unknown producer formula must not be aggregated", result.getKsmOrdinaryBytes());
        assertEquals(0, coverage(result, "ksmOrdinaryBytes").get("coveredHosts").getAsInt());
    }

    @Test public void disabledWithoutValueIsNotMetricCoverage() {
        String fields = partial("\"metricQuality\":{\"zram\":\"disabled\"}");
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Collections.singletonList("a"),
                Collections.singletonList(state("a", fields, "{")), NOW);
        JsonObject coverage = coverage(result, "zramBytes");
        assertEquals(0, coverage.get("coveredHosts").getAsInt());
        assertEquals("Missing", coverage.get("quality").getAsString());
        assertNull(result.getZramBytes());
    }

    @Test public void disabledAndObservedHostsKeepObservedCoverageAndPartialQuality() {
        MemoryStateVO observed = state("a", partial("\"zramBytes\":10,\"metricQuality\":{\"zram\":\"observed\"}"), "{");
        MemoryStateVO disabled = state("b", partial("\"metricQuality\":{\"zram\":\"disabled\"}"), "{");
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Arrays.asList("a", "b"), Arrays.asList(observed, disabled), NOW);
        JsonObject coverage = coverage(result, "zramBytes");
        assertEquals(1, coverage.get("coveredHosts").getAsInt());
        assertEquals("Partial", coverage.get("quality").getAsString());
        assertEquals(Long.valueOf(10), result.getZramBytes());
    }

    @Test public void overflowInOneMetricDoesNotEraseIndependentValidMetric() {
        MemoryStateVO overflow = state("a", partial("\"ksmOrdinaryBytes\":9223372036854775807"), "{");
        MemoryStateVO valid = state("b", partial("\"ksmOrdinaryBytes\":1,\"zramBytes\":5"), "{");
        MemorySummaryInventory result = MemorySummaryRules.summarize(
                Arrays.asList("a", "b"), Arrays.asList(overflow, valid), NOW);
        assertNull("overflowed ordinary metric must be missing", result.getKsmOrdinaryBytes());
        assertEquals("independent zram metric must survive ordinary overflow", Long.valueOf(5), result.getZramBytes());
        assertEquals(1, coverage(result, "zramBytes").get("coveredHosts").getAsInt());
    }

    @Test public void unknownNativeKsmQualityWithReasonRemainsHostError() {
        String prefix = "{\"capabilities\":{\"zram\":false,\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"},"
                + "\"actual\":{\"ksm\":{\"quality\":\"unknown\",\"reason\":\"native read failed\"}},";
        Map<String, Integer> status = MemorySummaryRules.summarize(
                Collections.singletonList("a"),
                Collections.singletonList(state("a", partial(""), prefix)), NOW).getCurrentStatus();
        assertEquals(Integer.valueOf(1), status.get("error"));
    }

    @Test public void unsupportedCapabilityDoesNotHideErrorWhenZramIsActuallyEnabled() {
        String prefix = "{\"capabilities\":{\"zram\":false,\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"},"
                + "\"actual\":{\"ksm\":{\"enabled\":false},\"zram\":{\"efficiencyEnabled\":true,"
                + "\"errors\":[{\"operation\":\"capacity\",\"reason\":\"[Errno 2] No such file or directory\"}]}},";
        Map<String, Integer> status = MemorySummaryRules.summarize(
                Collections.singletonList("a"),
                Collections.singletonList(state("a", partial(""), prefix)), NOW).getCurrentStatus();
        assertEquals(Integer.valueOf(1), status.get("error"));
    }

    @Test public void nativeKsmNumericParametersMustBeIntegers() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setHostUuid("host-ksm"); state.setControlOperationUuid("op-ksm");
        state.setStatus("Succeeded"); state.setDesiredRevision(2L); state.setAppliedRevision(2L);
        state.setLastSampleTime(100000L);
        state.setState("{\"managed\":true,\"ksmOnly\":true,\"bootId\":\"boot-1\","
                + "\"actual\":{\"ksm\":{\"enabled\":true,\"pagesToScan\":1.5,"
                + "\"sleepMillis\":20,\"zeroPagesEnabled\":false}}}");
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Unknown", state.getFeatureState());
    }

    @Test public void explicitDisabledZeroRetainsScalarAndCoverage() {
        MemorySummaryInventory result = MemorySummaryRules.summarize(Collections.singletonList("a"),
                Collections.singletonList(state("a", partial("\"zramBytes\":0,\"metricQuality\":{\"zram\":\"disabled\"}"), "{")), NOW);
        assertEquals("producer's explicit zero is a real value, not an inferred one", Long.valueOf(0), result.getZramBytes());
        assertEquals(1, coverage(result,"zramBytes").get("coveredHosts").getAsInt());
        assertEquals("Disabled", coverage(result,"zramBytes").get("quality").getAsString());
    }

    @Test public void ordinaryOverflowCannotBeOverwrittenByTotalSeriesAsZero() {
        String full = "\"formulaVersion\":\"mechanism-estimate-v1\",\"quality\":\"Fresh\",\"sampleTime\":"+NOW
                +",\"ksmOrdinaryBytes\":9223372036854775807,\"ksmZeroBytes\":0,\"ksmTotalBytes\":9223372036854775807,"
                +"\"zramBytes\":-9223372036854775807,\"totalSavedEstimateBytes\":0";
        MemorySummaryInventory result = MemorySummaryRules.summarize(Arrays.asList("a","b"), Arrays.asList(
                state("a",full,"{"), state("b",partial("\"ksmOrdinaryBytes\":1"),"{")), NOW);
        assertNull("overflowed independent ordinary series must not be replaced with zero",result.getKsmOrdinaryBytes());
        assertEquals(Long.valueOf(0),result.getTotalSavedEstimateBytes());
        assertEquals(1,result.getCoveredHosts());
    }

    @Test public void splitOverflowDoesNotDiscardIndependentOrdinarySample() {
        String fields="\"ksmOrdinaryBytes\":100,\"zramBytes\":0,\"zramNormalBytes\":9223372036854775807,\"zramWritebackBytes\":1";
        MemorySummaryInventory result=MemorySummaryRules.summarize(Collections.singletonList("a"),
                Collections.singletonList(state("a",partial(fields),"{")),NOW);
        assertEquals(Long.valueOf(100),result.getKsmOrdinaryBytes());
        assertEquals(1,coverage(result,"ksmOrdinaryBytes").get("coveredHosts").getAsInt());
    }

    @Test public void topLevelActualErrorsAreNeverOptionalComponentErrors() {
        String prefix="{\"capabilities\":{\"zram\":false,\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"},"
                +"\"actual\":{\"errors\":[{\"reason\":\"[Errno 2] No such file or directory\"}]},";
        MemorySummaryInventory result=MemorySummaryRules.summarize(Collections.singletonList("a"),
                Collections.singletonList(state("a",partial(""),prefix)),NOW);
        assertEquals(Integer.valueOf(1),result.getCurrentStatus().get("error"));
    }

    @Test public void explicitDisabledZeroPageMetricKeepsItsScalar() {
        MemorySummaryInventory r=summary(partial("\"ksmZeroBytes\":0,\"metricQuality\":{\"ksm_zero\":\"disabled\"}"));
        assertEquals(Long.valueOf(0),r.getKsmZeroBytes());
        assertEquals(1,coverage(r,"ksmZeroBytes").get("coveredHosts").getAsInt());
        assertEquals("Disabled",coverage(r,"ksmZeroBytes").get("quality").getAsString());
    }

    @Test public void disabledNonzeroValuesNeverCountAsExplicitZero() {
        for (long v : new long[]{-1,1}) {
            MemorySummaryInventory r=summary(partial("\"ksmZeroBytes\":"+v+",\"zramBytes\":"+v
                    +",\"metricQuality\":{\"ksm_zero\":\"disabled\",\"zram\":\"disabled\"}"));
            assertNull(r.getKsmZeroBytes());assertNull(r.getZramBytes());
            assertEquals(0,coverage(r,"ksmZeroBytes").get("coveredHosts").getAsInt());
            assertEquals(0,coverage(r,"zramBytes").get("coveredHosts").getAsInt());
        }
    }

    @Test public void malformedTotalFormulaDoesNotDiscardIndependentCounters() {
        String s="\"formulaVersion\":\"mechanism-estimate-v1\",\"quality\":\"Fresh\",\"sampleTime\":"+NOW
                +",\"ksmOrdinaryBytes\":9223372036854775807,\"ksmZeroBytes\":1,\"ksmTotalBytes\":9223372036854775807,"
                +"\"zramBytes\":2,\"totalSavedEstimateBytes\":0";
        MemorySummaryInventory r=summary(s);
        assertEquals(Long.valueOf(Long.MAX_VALUE),r.getKsmOrdinaryBytes());
        assertEquals(Long.valueOf(1),r.getKsmZeroBytes());
        assertEquals(Long.valueOf(2),r.getZramBytes());
        assertNull(r.getTotalSavedEstimateBytes());
    }

    @Test public void optionalZramUnknownPermissionErrorIsStillBlocking() {
        assertBlocking("\"zram\":{\"quality\":\"unknown\",\"reason\":\"[Errno 13] Permission denied\"}");
    }

    @Test public void enabledWritebackReadErrorIsNeverOptional() {
        assertBlocking("\"writeback\":{\"enabled\":true,\"errors\":[{\"operation\":\"backend-state\",\"reason\":\"[Errno 2] No such file or directory\"}]}");
    }

    @Test public void nestedZramNameUnderKsmDoesNotBecomeOptional() {
        assertBlocking("\"ksm\":{\"zram\":{\"errors\":[{\"reason\":\"[Errno 2] No such file or directory\"}]}}");
    }

    @Test public void errnoTwentyIsNotErrnoTwo() {
        assertBlocking("\"zram\":{\"errors\":[{\"operation\":\"capacity-state\",\"reason\":\"[Errno 20] Not a directory\"}]}");
    }

    @Test public void malformedPerMetricQualityIsNotImplicitlyObserved() {
        MemorySummaryInventory r=summary(partial("\"ksmOrdinaryBytes\":100,\"metricQuality\":{\"ksm_ordinary\":{\"quality\":\"missing\"}}"));
        assertNull(r.getKsmOrdinaryBytes());
        assertEquals(0,coverage(r,"ksmOrdinaryBytes").get("coveredHosts").getAsInt());
    }

    private MemorySummaryInventory summary(String savings) {
        return MemorySummaryRules.summarize(Collections.singletonList("a"),Collections.singletonList(state("a",savings,"{")),NOW);
    }

    @Test public void subtotalOnlyStillHasPartialQualityAndSampleTime() {
        MemorySummaryInventory r=summary(partial("\"ksmTotalBytes\":42"));
        assertEquals(Long.valueOf(42),r.getKsmTotalBytes());
        assertEquals("Partial",r.getQuality());
        assertEquals(Long.valueOf(NOW),r.getSampleTime());
    }

    @Test public void explicitDisabledZeroStillHasSampleTime() {
        MemorySummaryInventory r=summary(partial("\"zramBytes\":0,\"metricQuality\":{\"zram\":\"disabled\"}"));
        assertEquals(Long.valueOf(NOW),r.getSampleTime());
    }

    @Test public void disabledZeroCannotMaskObservedAggregationOverflow() {
        for (String field : new String[]{"ksmZeroBytes","zramBytes"}) {
            String quality=field.equals("ksmZeroBytes")?"ksm_zero":"zram";
            MemoryStateVO a=state("a",partial("\""+field+"\":9223372036854775807"),"{");
            MemoryStateVO b=state("b",partial("\""+field+"\":1"),"{");
            MemoryStateVO c=state("c",partial("\""+field+"\":0,\"metricQuality\":{\""+quality+"\":\"disabled\"}"),"{");
            MemorySummaryInventory r=MemorySummaryRules.summarize(Arrays.asList("a","b","c"),Arrays.asList(a,b,c),NOW);
            assertNull("invalid observed sum must not be replaced with disabled zero",field.equals("ksmZeroBytes")?r.getKsmZeroBytes():r.getZramBytes());
            assertEquals(0,coverage(r,field).get("coveredHosts").getAsInt());
            assertEquals("overflowed observed series is unknown, not all-disabled","Unknown",coverage(r,field).get("quality").getAsString());
        }
    }

    @Test public void splitAggregateOverflowDoesNotEraseValidNetTotalSeries() {
        String s="\"formulaVersion\":\"mechanism-estimate-v1\",\"quality\":\"Fresh\",\"sampleTime\":"+NOW
                +",\"ksmOrdinaryBytes\":0,\"ksmZeroBytes\":0,\"ksmTotalBytes\":0,\"zramBytes\":0,\"totalSavedEstimateBytes\":0,"
                +"\"zramNormalBytes\":9223372036854775807,\"zramWritebackBytes\":-9223372036854775807";
        MemorySummaryInventory r=MemorySummaryRules.summarize(Arrays.asList("a","b"),Arrays.asList(state("a",s,"{"),state("b",s,"{")),NOW);
        assertEquals(Long.valueOf(0),r.getTotalSavedEstimateBytes());
        assertEquals(2,r.getCoveredHosts());
        assertNull(r.getZramNormalBytes());assertNull(r.getZramWritebackBytes());
    }

    @Test public void explicitDisabledZerosCanCompleteAnOtherwiseValidTotalSeries() {
        for (String zeroQuality : new String[]{"observed","disabled"}) {
            String s="\"formulaVersion\":\"mechanism-estimate-v1\",\"quality\":\"Fresh\",\"sampleTime\":"+NOW
                    +",\"ksmOrdinaryBytes\":50,\"ksmZeroBytes\":0,\"ksmTotalBytes\":50,\"zramBytes\":0,\"totalSavedEstimateBytes\":50,"
                    +"\"metricQuality\":{\"ksm_ordinary\":\"observed\",\"ksm_zero\":\""+zeroQuality+"\",\"ksm_total\":\"observed\",\"zram\":\"disabled\"}";
            MemorySummaryInventory r=summary(s);
            assertEquals(Long.valueOf(50),r.getTotalSavedEstimateBytes());
            assertEquals(1,r.getCoveredHosts());assertEquals("Fresh",r.getQuality());
        }
    }

    private void assertBlocking(String actual) {
        String prefix="{\"capabilities\":{\"zram\":false,\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"},\"actual\":{"+actual+"},";
        MemorySummaryInventory r=MemorySummaryRules.summarize(Collections.singletonList("a"),Collections.singletonList(state("a",partial(""),prefix)),NOW);
        assertEquals(Integer.valueOf(1),r.getCurrentStatus().get("error"));
    }

    private JsonObject coverage(MemorySummaryInventory result, String key) {
        JsonObject root = JsonParser.parseString(new Gson().toJson(result)).getAsJsonObject();
        return root.getAsJsonObject("metricCoverage").getAsJsonObject(key);
    }
}
