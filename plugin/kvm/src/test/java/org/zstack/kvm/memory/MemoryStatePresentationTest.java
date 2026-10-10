package org.zstack.kvm.memory;

import org.junit.Test;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static org.junit.Assert.*;

public class MemoryStatePresentationTest {
    private MemoryStateInventory runningGoState() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setHostUuid("host-1"); state.setControlOperationUuid("op-1");
        state.setStatus("Succeeded"); state.setDesiredRevision(31); state.setAppliedRevision(31L);
        state.setLastSampleTime(100000L);
        state.setState("{\"managed\":true,\"bootId\":\"boot-1\","
                + "\"lastConfirmedOperationUuid\":\"op-1\",\"lastConfirmedAppliedRevision\":31,"
                + "\"lifecycle\":{\"activeState\":\"compatibility\",\"paused\":false,\"desiredRevision\":23},"
                + "\"poolGeneration\":\"pool-1\",\"host_zram\":{\"pool_generation\":\"pool-1\","
                + "\"device\":\"/dev/zram0\",\"quality\":\"native_host_device\","
                + "\"observed_at\":\"1970-01-01T00:01:40Z\"},"
                + "\"actual\":{\"zram\":{\"efficiencyEnabled\":true},\"ksm\":{\"enabled\":true}}}");
        return state;
    }

    @Test public void runningGoPoolOffersPauseAndDrainWithoutTreatingLegacyLabelAsDisabled() {
        MemoryStateInventory state = runningGoState();
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Active", state.getFeatureState());
        assertEquals("GO_LIFECYCLE", state.getFeatureStateSource());
        assertEquals(java.util.Arrays.asList("query", "pause", "drain"), state.getAllowedActions());
        assertTrue("native Go section revision is not a Cloud revision", state.getState().contains("\"desiredRevision\":23"));
    }

    @Test public void pausedPoolAllowsDrainButResumeStillRequiresRepositoryProof() {
        MemoryStateInventory state = runningGoState();
        state.setState(state.getState().replace("\"compatibility\"", "\"paused\"").replace("\"paused\":false", "\"paused\":true"));
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals(java.util.Arrays.asList("query", "drain"), state.getAllowedActions());
        MemoryStatePresentation.populate(state, 100100L, true, false);
        assertEquals(java.util.Arrays.asList("query", "drain", "resume"), state.getAllowedActions());
    }

    @Test public void stoppedOptimizationWithRemainingPoolCanStillBeDrained() {
        MemoryStateInventory state = runningGoState();
        state.setState(state.getState().replace(":true", ":false").replace("\"managed\":false", "\"managed\":true"));
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Disabled", state.getFeatureState());
        assertTrue(state.getAllowedActions().contains("drain"));
    }

    @Test public void uncertainStaleOrUninitializedGoStateNeverOffersPoolActions() {
        for (String defect : new String[]{"unknown", "busy", "revision", "control", "boot", "unmanaged", "ksmOnly",
                "pool", "generation", "nativeStale", "future", "hold", "pausedMismatch", "sampleStale"}) {
            MemoryStateInventory state = runningGoState();
            if ("unknown".equals(defect)) state.setStatus("Unknown");
            if ("busy".equals(defect)) state.setActiveTaskUuid("pending");
            if ("revision".equals(defect)) state.setAppliedRevision(30L);
            if ("control".equals(defect)) state.setControlOperationUuid("different");
            if ("boot".equals(defect)) state.setState(state.getState().replace("\"boot-1\"", "\"\""));
            if ("unmanaged".equals(defect)) state.setState(state.getState().replace("\"managed\":true", "\"managed\":false"));
            if ("ksmOnly".equals(defect)) state.setState(state.getState().replace("\"managed\":true", "\"managed\":true,\"ksmOnly\":true"));
            if ("pool".equals(defect)) state.setState(state.getState().replace("\"host_zram\"", "\"absent_pool\""));
            if ("generation".equals(defect)) state.setState(state.getState().replace("\"poolGeneration\":\"pool-1\"", "\"poolGeneration\":\"different\""));
            if ("nativeStale".equals(defect)) state.setState(state.getState().replace("00:01:40", "00:00:10"));
            if ("future".equals(defect)) state.setState(state.getState().replace("00:01:40", "00:02:40"));
            if ("hold".equals(defect)) state.setState(state.getState().replace("\"paused\":false", "\"paused\":false,\"migrationHold\":{}"));
            if ("pausedMismatch".equals(defect)) state.setState(state.getState().replace("\"paused\":false", "\"paused\":true"));
            if ("sampleStale".equals(defect)) state.setLastSampleTime(10000L);
            MemoryStatePresentation.populate(state, 100100L);
            assertEquals(defect, java.util.Collections.singletonList("query"), state.getAllowedActions());
            assertNotEquals(defect, "Active", state.getFeatureState());
        }
    }

    @Test public void presentationExposesActualTtlWithoutRefreshingTheSample() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setLastSampleTime(1000L);
        String nativeState = "{\"savings\":{\"quality\":\"Fresh\",\"sampleTime\":1000,\"zramBytes\":0}}";
        state.setState(nativeState);
        MemoryStatePresentation.populate(state, 2000L);
        assertEquals(MemoryOptimizationGlobalConfig.displayTtlMillis(),
                ((Number) state.getMetrics().get("displayTtlMillis")).longValue());
        assertEquals(2000L, ((Number) state.getMetrics().get("presentationTime")).longValue());
        assertEquals(1000L, ((Number) state.getMetrics().get("sampleTime")).longValue());
        assertEquals(Long.valueOf(1000), state.getLastSampleTime());
        assertEquals(nativeState, state.getState());
        assertEquals(0L, ((Number) state.getMetrics().get("zramBytes")).longValue());
        MemoryStatePresentation.populate(state, 91000L);
        assertEquals("Stale", state.getQuality());
        assertEquals(1000L, ((Number) state.getMetrics().get("sampleTime")).longValue());
    }

    @Test public void exactTtlBoundaryIsStale() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setLastSampleTime(1000L);
        state.setState("{\"savings\":{\"quality\":\"Fresh\"}}");
        MemoryStatePresentation.populate(state, 91000L);
        assertEquals("Stale", state.getQuality());
    }
    @Test public void staleSampleIsNotFreshAndNegativeEstimateIsPreserved() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setLastSampleTime(1000L); state.setDesiredRevision(2); state.setAppliedRevision(1L);
        state.setState("{\"bootId\":\"boot\",\"lifecycle\":{\"activeState\":\"Paused\"},"
                + "\"savings\":{\"quality\":\"Fresh\",\"totalSavedEstimateBytes\":-16}}");
        MemoryStatePresentation.populate(state, 92000);
        assertEquals("Stale", state.getQuality());
        assertEquals(Long.valueOf(91), state.getDataAgeSeconds());
        assertEquals(Boolean.TRUE, state.getDrift());
        assertEquals("Paused", state.getFeatureState());
        assertEquals(-16, ((Number) state.getMetrics().get("totalSavedEstimateBytes")).intValue());
    }
    @Test public void missingOrFutureSampleDoesNotBecomeZeroOrFresh() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setState("not-json"); MemoryStatePresentation.populate(state, 1000);
        assertEquals("Unknown", state.getQuality()); assertNull(state.getDrift());
        assertNull(state.getDataAgeSeconds()); assertTrue(state.getMetrics().isEmpty());
        state.setLastSampleTime(7000L); MemoryStatePresentation.populate(state, 1000);
        assertEquals("Unknown", state.getQuality()); assertNull(state.getDataAgeSeconds());
        assertEquals(java.util.Collections.singletonList("query"), state.getAllowedActions());
    }

    @Test public void ksmOnlyNativeProofIsActiveWithoutSynthesizingGoLifecycle() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setHostUuid("host-ksm"); state.setControlOperationUuid("op-ksm"); state.setStatus("Succeeded");
        state.setDesiredRevision(7L); state.setAppliedRevision(7L); state.setLastSampleTime(100000L);
        state.setState("{\"managed\":true,\"ksmOnly\":true,\"bootId\":\"boot-1\","
                + "\"actual\":{\"ksm\":{\"enabled\":true,\"pagesToScan\":100,\"sleepMillis\":20,\"zeroPagesEnabled\":false}},"
                + "\"savings\":{\"quality\":\"Partial\",\"sampleTime\":100000,"
                + "\"ksmOrdinaryBytes\":4096}}");
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Active", state.getFeatureState());
        assertEquals("Partial", state.getQuality());
        String json = new Gson().toJson(state);
        assertTrue("the source of Active must be explicit", json.contains("featureStateSource"));
        assertTrue(json.contains("KSM_NATIVE"));
        assertFalse("KSM-only proof must not invent a Go lifecycle", state.getState().contains("lifecycle"));
    }

    @Test public void queryOnlyAllowedActionsDoesNotMeanDisabledForKsmOnly() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setHostUuid("host-ksm"); state.setControlOperationUuid("op-ksm");
        state.setStatus("Succeeded"); state.setDesiredRevision(2L); state.setAppliedRevision(2L);
        state.setLastSampleTime(100000L);
        state.setState("{\"managed\":true,\"ksmOnly\":true,\"bootId\":\"boot-1\","
                + "\"actual\":{\"ksm\":{\"enabled\":true,\"pagesToScan\":100,\"sleepMillis\":20,\"zeroPagesEnabled\":false}},\"savings\":{\"quality\":\"Fresh\","
                + "\"sampleTime\":100000,\"totalSavedEstimateBytes\":-1}}");
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Active", state.getFeatureState());
        assertEquals(java.util.Collections.singletonList("query"), state.getAllowedActions());
        assertNotEquals("Disabled", state.getFeatureState());
    }

    @Test public void ksmOnlyRevisionDriftDoesNotBecomeActive() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setHostUuid("host-ksm"); state.setControlOperationUuid("op-ksm");
        state.setStatus("Succeeded"); state.setDesiredRevision(8L); state.setAppliedRevision(7L);
        state.setLastSampleTime(100000L);
        state.setState("{\"managed\":true,\"ksmOnly\":true,\"bootId\":\"boot-1\","
                + "\"actual\":{\"ksm\":{\"enabled\":true,\"pagesToScan\":100,\"sleepMillis\":20,\"zeroPagesEnabled\":false}},\"savings\":{\"quality\":\"Fresh\","
                + "\"sampleTime\":100000}}");
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Unknown", state.getFeatureState());
    }

    @Test public void validKsmOnlyNativeDisabledIsDisabledWithoutGoLifecycle() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setHostUuid("host-ksm"); state.setControlOperationUuid("op-ksm");
        state.setStatus("Succeeded"); state.setDesiredRevision(8L); state.setAppliedRevision(8L);
        state.setLastSampleTime(100000L);
        state.setState("{\"managed\":true,\"ksmOnly\":true,\"bootId\":\"boot-1\","
                + "\"actual\":{\"ksm\":{\"enabled\":false,\"pagesToScan\":100,\"sleepMillis\":20,\"zeroPagesEnabled\":false}},\"savings\":{\"quality\":\"Fresh\","
                + "\"sampleTime\":100000}}");
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Disabled", state.getFeatureState());
        String json = new Gson().toJson(state);
        assertTrue(json.contains("featureStateSource")); assertTrue(json.contains("KSM_NATIVE"));
    }

    @Test public void realKylinOffObservationReplaysAsDisabledKsmNative() throws Exception {
        JsonObject root = MemoryKylinFixture.nativeOff();
        JsonObject inventory = null;
        for (com.google.gson.JsonElement item : root.getAsJsonObject("body").getAsJsonArray("inventories")) {
            JsonObject candidate = item.getAsJsonObject();
            if ("ae99e7222906497281fb3d4141f78833".equals(candidate.get("hostUuid").getAsString())) {
                inventory = candidate; break;
            }
        }
        assertNotNull(inventory);
        MemoryStateInventory state = new MemoryStateInventory();
        state.setHostUuid(inventory.get("hostUuid").getAsString());
        state.setControlOperationUuid(inventory.get("controlOperationUuid").getAsString());
        state.setStatus(inventory.get("status").getAsString());
        state.setDesiredRevision(inventory.get("desiredRevision").getAsLong());
        state.setAppliedRevision(inventory.get("appliedRevision").getAsLong());
        state.setLastSampleTime(inventory.get("lastSampleTime").getAsLong());
        state.setState(inventory.get("state").getAsString());
        MemoryStatePresentation.populate(state, inventory.getAsJsonObject("metrics").get("presentationTime").getAsLong());
        assertEquals("Disabled", state.getFeatureState());
        assertEquals("Partial", state.getQuality());
        assertEquals(java.util.Collections.singletonList("query"), state.getAllowedActions());
    }

    @Test public void ksmOnlyExpiredNativeObservationDoesNotBecomeActive() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setHostUuid("host-ksm"); state.setControlOperationUuid("op-ksm");
        state.setStatus("Succeeded"); state.setDesiredRevision(8L); state.setAppliedRevision(8L);
        state.setLastSampleTime(1000L);
        state.setState("{\"managed\":true,\"ksmOnly\":true,\"bootId\":\"boot-1\","
                + "\"actual\":{\"ksm\":{\"enabled\":true,\"pagesToScan\":100,\"sleepMillis\":20,\"zeroPagesEnabled\":false}},\"savings\":{\"quality\":\"Fresh\","
                + "\"sampleTime\":1000}}");
        MemoryStatePresentation.populate(state, 91000L);
        assertEquals("Unknown", state.getFeatureState());
        assertEquals("Stale", state.getQuality());
    }

    @Test public void ksmOnlyMissingBootOrNativeEvidenceRemainsUnknown() {
        for (String evidence : new String[]{
                "{\"managed\":true,\"ksmOnly\":true,\"actual\":{\"ksm\":{\"enabled\":true}}}",
                "{\"managed\":true,\"ksmOnly\":true,\"bootId\":\"boot-1\",\"actual\":{}}"}) {
            MemoryStateInventory state = new MemoryStateInventory();
            state.setHostUuid("host-ksm"); state.setControlOperationUuid("op-ksm");
            state.setStatus("Succeeded"); state.setDesiredRevision(8L); state.setAppliedRevision(8L);
            state.setLastSampleTime(100000L); state.setState(evidence);
            MemoryStatePresentation.populate(state, 100100L);
            assertEquals("Unknown", state.getFeatureState());
        }
    }

    @Test public void ksmOnlyNonBooleanNativeEnabledIsUnknown() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setHostUuid("host-ksm"); state.setControlOperationUuid("op-ksm");
        state.setStatus("Succeeded"); state.setDesiredRevision(8L); state.setAppliedRevision(8L);
        state.setLastSampleTime(100000L);
        state.setState("{\"managed\":true,\"ksmOnly\":true,\"bootId\":\"boot-1\","
                + "\"actual\":{\"ksm\":{\"enabled\":\"true\",\"pagesToScan\":100,"
                + "\"sleepMillis\":20,\"zeroPagesEnabled\":false}},"
                + "\"savings\":{\"quality\":\"Fresh\",\"sampleTime\":100000}}");
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Unknown", state.getFeatureState());
    }
}
