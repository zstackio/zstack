package org.zstack.kvm.memory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

/** Updated product contracts; tests the actual policy entry, not a duplicate model. */
public class MemoryPolicyRevisionTest {
    private String merge(String policy, String scope) {
        return MemoryPolicyRules.merge("{}", policy, scope);
    }

    private void rejects(String policy) {
        try {
            merge(policy, "Host");
            fail("Invalid policy accepted: " + policy);
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("MEMORY_INVALID_POLICY"));
        }
    }

    @Test public void removedKsmControlsAreRejectedOnNewRequests() {
        rejects("{\"ksm\":{\"mode\":\"Active\"}}");
        rejects("{\"ksm\":{\"profileVersion\":\"active-v1\"}}");
        rejects("{\"ksm\":{\"cpuBudgetPercent\":20}}");
    }

    @Test public void clusterAcceptsSamePartialOverrideAsHost() {
        String policy = "{\"ksm\":{\"enabled\":false,\"zeroPagesEnabled\":true}}";
        assertEquals(merge(policy, "Host"), merge(policy, "Cluster"));
    }

    @Test public void ksmHasActualUintBoundsNotCandidateProductCeilings() {
        JsonObject policy = new JsonParser().parse(merge(
                "{\"ksm\":{\"pagesToScan\":4294967295,\"sleepMillis\":4294967295}}",
                "Host")).getAsJsonObject();
        assertEquals(4294967295L, policy.getAsJsonObject("ksm").get("pagesToScan").getAsLong());
        rejects("{\"ksm\":{\"pagesToScan\":4294967296}}");
        rejects("{\"ksm\":{\"sleepMillis\":4294967296}}");
        rejects("{\"ksm\":{\"sleepMillis\":9}}");
        rejects("{\"ksm\":{\"pagesToScan\":0}}");
    }

    @Test public void capacitiesDoNotUseOneTibOr512GibProductCaps() {
        merge("{\"zram\":{\"logicalCapacityBytes\":2199023255552,\"ramLimitBytes\":1099511627776}}", "Host");
        merge("{\"writeback\":{\"backendCapacityBytes\":2199023255552,\"batchBytes\":134217728}}", "Host");
        rejects("{\"zram\":{\"logicalCapacityBytes\":4097}}");
        rejects("{\"zram\":{\"logicalCapacityBytes\":9223372036854775808}}");
    }

    @Test public void reserveDefaultsAreNotMandatoryMinimumsOrPageSizes() {
        merge("{\"zram\":{\"hostFloorBytes\":6871947673,\"transientReserveBytes\":0,\"metadataReserveBytes\":0}}", "Host");
        rejects("{\"zram\":{\"hostFloorBytes\":-1}}");
    }

    @Test public void longWritebackTimingIsAcceptedButNanosecondOverflowIsNot() {
        merge("{\"writeback\":{\"idleObservationSeconds\":7200,\"operationIntervalSeconds\":7200}}", "Host");
        rejects("{\"writeback\":{\"idleObservationSeconds\":9223372037}}");
    }

    @Test public void defaultsContainNoObsoleteStrategyFields() {
        JsonObject defaults = new JsonParser().parse(MemoryPolicyRules.defaults()).getAsJsonObject();
        JsonObject ksm = defaults.getAsJsonObject("ksm");
        assertFalse(ksm.has("mode"));
        assertFalse(ksm.has("cpuBudgetPercent"));
        assertFalse(ksm.has("profileVersion"));
    }

    @Test public void configurableServiceControlsSurviveTypedPolicySerialization() {
        String advanced = "{\"zram\":{\"selectionMode\":\"list\",\"selectedVmUuids\":[],"
                + "\"discoveryIntervalSeconds\":60,\"discoveryTimeoutSeconds\":30,\"discoveryTtlSeconds\":120,"
                + "\"hostSampleIntervalSeconds\":2,\"hostSampleTtlSeconds\":3,\"vmSampleIntervalSeconds\":2,\"vmSampleTtlSeconds\":6,"
                + "\"hostCpuGuardEnabled\":false,\"hostMemoryPsiGuardEnabled\":true,\"hostIoPsiGuardEnabled\":true,"
                + "\"hostCpuThresholdPercent\":50,\"hostMemoryPsiThresholdPercent\":0.1,\"hostIoPsiThresholdPercent\":1,"
                + "\"reclaimBatchBytes\":67108864,\"concurrency\":2,\"timeoutIsolationSlots\":10,"
                + "\"reclaimSlowOperationSeconds\":60,\"operationRecordBudgetBytes\":1073741824,"
                + "\"startupObservationSeconds\":0,\"algorithm\":\"lzo\"},"
                + "\"writeback\":{\"slowOperationSeconds\":60,\"noProgressLimit\":3,\"backoffSeconds\":60,"
                + "\"ioErrorBackoffSeconds\":60,\"metadataBudgetBytes\":1073741824,\"filesystemReserveBytes\":0}}";
        JsonObject before = new JsonParser().parse(advanced).getAsJsonObject();
        JsonObject after = new JsonParser().parse(merge(advanced, "Host")).getAsJsonObject();
        before.entrySet().forEach(section -> assertEquals(section.getValue(), after.get(section.getKey())));
    }

    @Test public void selectionListsDoNotHave256Or1024CeilingsAndInvalidIdsReject() {
        com.google.gson.JsonArray ids = new com.google.gson.JsonArray();
        for (int i = 0; i < 3000; i++) { ids.add(String.format("%032x", i)); }
        String request = "{\"zram\":{\"selectionMode\":\"list\",\"selectedVmUuids\":" + ids + "}}";
        assertEquals(3000, new JsonParser().parse(merge(request, "Host")).getAsJsonObject()
                .getAsJsonObject("zram").getAsJsonArray("selectedVmUuids").size());
        rejects("{\"zram\":{\"selectionMode\":\"whitelist\"}}");
        rejects("{\"zram\":{\"selectedVmUuids\":[\"not-a-vm\"]}}");
        rejects("{\"zram\":{\"timeoutIsolationSlots\":-1}}");
        rejects("{\"zram\":{\"operationRecordBudgetBytes\":0}}");
        rejects("{\"zram\":{\"hostMemoryPsiThresholdPercent\":100.1}}");
        rejects("{\"zram\":{\"hostCpuGuardEnabled\":\"false\"}}");
    }
}
