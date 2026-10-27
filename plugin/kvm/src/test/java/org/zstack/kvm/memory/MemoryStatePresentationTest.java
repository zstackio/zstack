package org.zstack.kvm.memory;

import org.junit.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static org.junit.Assert.*;

public class MemoryStatePresentationTest {
    private MemoryStateInventory observerKsmState(boolean enabled) {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setHostUuid("host-1"); state.setControlOperationUuid("op-1");
        state.setStatus("Succeeded"); state.setDesiredRevision(31); state.setAppliedRevision(31L);
        state.setLastSampleTime(100000L);
        state.setState("{\"managed\":true,\"ksmOnly\":true,\"bootId\":\"boot-1\","
                + "\"lastConfirmedOperationUuid\":\"op-1\",\"lastConfirmedAppliedRevision\":31,"
                + "\"mode\":\"observe\",\"resource_authorized\":false,"
                + "\"lifecycle\":{\"activeState\":\"compatibility\",\"paused\":false},"
                + "\"host_zram\":{\"pool_generation\":\"\",\"device\":\"\",\"quality\":\"unavailable\"},"
                + "\"actual\":{\"ksm\":{\"enabled\":" + enabled
                + ",\"pagesToScan\":2000,\"sleepMillis\":20,\"zeroPagesEnabled\":false}}}");
        return state;
    }

    @Test public void reachableObserverUsesConfirmedKsmProofWithoutOfferingPoolActions() {
        for (boolean enabled : new boolean[]{true, false}) {
            MemoryStateInventory state = observerKsmState(enabled);
            MemoryStatePresentation.populate(state, 100100L);
            assertEquals(enabled ? "Active" : "Disabled", state.getFeatureState());
            assertEquals(java.util.Collections.singletonList("query"), state.getAllowedActions());
        }
    }

    @Test public void observerKsmProofRejectsMissingFencesAndNativePoolContradictions() {
        for (String defect : new String[]{"control", "revision", "busy", "unknown", "stale",
                "boot", "mode", "authorized", "pool", "hold", "paused", "maintenance"}) {
            MemoryStateInventory state = observerKsmState(true);
            JsonObject raw = new JsonParser().parse(state.getState()).getAsJsonObject();
            if ("control".equals(defect)) raw.addProperty("lastConfirmedOperationUuid", "other");
            if ("revision".equals(defect)) raw.addProperty("lastConfirmedAppliedRevision", 30);
            if ("busy".equals(defect)) state.setActiveTaskUuid("busy");
            if ("unknown".equals(defect)) state.setStatus("Unknown");
            if ("stale".equals(defect)) state.setLastSampleTime(1L);
            if ("boot".equals(defect)) raw.remove("bootId");
            if ("mode".equals(defect)) raw.addProperty("mode", "reclaim");
            if ("authorized".equals(defect)) raw.addProperty("resource_authorized", true);
            if ("pool".equals(defect)) raw.getAsJsonObject("host_zram").addProperty("pool_generation", "pool");
            if ("hold".equals(defect)) raw.getAsJsonObject("lifecycle").add("migrationHold", new JsonObject());
            if ("paused".equals(defect)) raw.getAsJsonObject("lifecycle").addProperty("paused", true);
            if ("maintenance".equals(defect)) raw.addProperty("maintenance_blocks_activation", true);
            state.setState(raw.toString());
            MemoryStatePresentation.populate(state, 100100L);
            assertEquals(defect, "Unknown", state.getFeatureState());
            assertEquals(defect, java.util.Collections.singletonList("query"), state.getAllowedActions());
        }
    }

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
        assertEquals(java.util.Arrays.asList("query", "pause", "drain"), state.getAllowedActions());
        assertTrue("native Go section revision is not a Cloud revision", state.getState().contains("\"desiredRevision\":23"));
    }

    @Test public void pausedPoolAllowsDrainButResumeStillRequiresRepositoryProof() {
        MemoryStateInventory state = runningGoState();
        state.setState(state.getState().replace("\"compatibility\"", "\"paused\"").replace("\"paused\":false", "\"paused\":true"));
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals(java.util.Arrays.asList("query", "drain"), state.getAllowedActions());
        MemoryStatePresentation.populate(state, 100100L, true);
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
        assertEquals("Paused", state.getFeatureState());
        assertEquals(-16, ((Number) state.getMetrics().get("totalSavedEstimateBytes")).intValue());
    }
    @Test public void missingOrFutureSampleDoesNotBecomeZeroOrFresh() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setState("not-json"); MemoryStatePresentation.populate(state, 1000);
        assertEquals("Unknown", state.getQuality()); assertTrue(state.getMetrics().isEmpty());
        state.setLastSampleTime(7000L); MemoryStatePresentation.populate(state, 1000);
        assertEquals("Unknown", state.getQuality());
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

    private static void attachPartial(MemoryStateInventory state, String boot, long sample, Boolean enabled) {
        JsonObject raw = new JsonParser().parse(state.getState()).getAsJsonObject();
        JsonObject partial = new JsonObject(); partial.addProperty("observationKind", "KSM_NATIVE_PARTIAL");
        partial.addProperty("bootId", boot); partial.addProperty("sampleTime", sample);
        JsonObject ksm = new JsonObject(); ksm.addProperty("enabled", enabled);
        ksm.addProperty("pagesToScan", 2000); ksm.addProperty("sleepMillis", 20);
        ksm.addProperty("zeroPagesEnabled", false); partial.add("ksm", ksm);
        raw.add("nativeKsmObservation", partial); state.setState(raw.toString());
    }

    @Test public void newerPartialBootOrKsmContradictionInvalidatesConfirmedState() {
        for (String defect : new String[]{"newBoot", "disabled", "missing", "future", "changedTuning"}) {
            MemoryStateInventory state = observerKsmState(true);
            attachPartial(state, "newBoot".equals(defect) ? "boot-2" : "boot-1",
                    "future".equals(defect) ? 100200L : 100050L,
                    "missing".equals(defect) ? null : !"disabled".equals(defect));
            if ("changedTuning".equals(defect)) {
                JsonObject raw = new JsonParser().parse(state.getState()).getAsJsonObject();
                raw.getAsJsonObject("nativeKsmObservation").getAsJsonObject("ksm").addProperty("pagesToScan", 3000);
                state.setState(raw.toString());
            }
            MemoryStatePresentation.populate(state, 100100L, true);
            assertEquals(defect, "Unknown", state.getFeatureState());
            assertEquals(defect, java.util.Collections.singletonList("query"), state.getAllowedActions());
            assertEquals(Long.valueOf(100000), state.getLastSampleTime());
            assertEquals("Succeeded", state.getStatus());
            if ("newBoot".equals(defect)) assertNull(state.getBootId());
        }
    }

    @Test public void olderPartialAndMatchingPartialDoNotOverwriteOrRenewFullProof() {
        MemoryStateInventory state = observerKsmState(true);
        attachPartial(state, "old-boot", 99999L, false);
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Active", state.getFeatureState());
        attachPartial(state, "boot-1", 100050L, true);
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Active", state.getFeatureState());
        MemoryStatePresentation.populate(state, 100000L + MemoryOptimizationGlobalConfig.displayTtlMillis());
        assertEquals("Unknown", state.getFeatureState());
        assertEquals(Long.valueOf(100000), state.getLastSampleTime());
    }

    @Test public void newerFullObservationRestoresStateAfterPartialContradiction() {
        MemoryStateInventory state = observerKsmState(true);
        attachPartial(state, "boot-2", 100050L, false);
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Unknown", state.getFeatureState());
        state.setLastSampleTime(100080L);
        state.setState(state.getState().replace("boot-1", "boot-2"));
        MemoryStatePresentation.populate(state, 100100L);
        assertEquals("Active", state.getFeatureState());
        assertEquals("boot-2", state.getBootId());
    }

}
