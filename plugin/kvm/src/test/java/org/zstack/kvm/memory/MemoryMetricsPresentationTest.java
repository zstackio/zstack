package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryMetricsPresentationTest {
    @Test public void prometheusFailureNeverFallsBackToCachedAgentSavings() {
        MemoryStateVO state = state();
        MemoryStateInventory result = MemoryMetricsPresentation.inventory(state, null, 100000);
        assertEquals("Unknown", result.getQuality());
        assertFalse(result.getMetrics().containsKey("totalSavedEstimateBytes"));
        assertTrue(state.getState().contains("1234"));
        assertEquals("Succeeded", result.getStatus());
    }

    @Test public void originalMetricTimeSurvivesFreshControlPoll() {
        Map<String, Object> savings = new HashMap<>();
        savings.put("sampleTime", 10000L); savings.put("quality", "Fresh");
        savings.put("totalSavedEstimateBytes", -20L);
        MemoryStateInventory result = MemoryMetricsPresentation.inventory(state(), savings, 100000);
        assertEquals("Stale", result.getQuality());
        assertEquals(Long.valueOf(90), result.getDataAgeSeconds());
        assertEquals(-20, ((Number) result.getMetrics().get("totalSavedEstimateBytes")).longValue());
    }

    @Test public void inventoryPreservesSourceControlAndCurrentStepWhileProjectingTrustedNativeFeature() {
        MemoryStateVO state = new MemoryStateVO();
        state.setHostUuid("host"); state.setStatus("Succeeded"); state.setReason("original");
        state.setPolicyPlanStatus("Blocked"); state.setPolicyPlanHash("plan");
        state.setActiveTaskUuid(null); state.setControlOperationUuid("op");
        state.setDesiredRevision(31); state.setAppliedRevision(31L); state.setCurrentStep("drain");
        state.setLastSampleTime(100000L);
        String nativeState = "{\"managed\":true,\"ksmOnly\":true,\"mode\":\"observe\","
                + "\"resource_authorized\":false,\"bootId\":\"boot-1\","
                + "\"lastConfirmedOperationUuid\":\"op\",\"lastConfirmedAppliedRevision\":31,"
                + "\"lifecycle\":{\"activeState\":\"compatibility\",\"paused\":false},"
                + "\"host_zram\":{\"pool_generation\":\"\",\"device\":\"\",\"quality\":\"unavailable\"},"
                + "\"actual\":{\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":false,"
                + "\"pagesToScan\":2000,\"sleepMillis\":20}}}";
        state.setState(nativeState);
        String sourceState = state.getState();
        MemoryStateInventory result = MemoryMetricsPresentation.inventory(state, null, 100100L);
        assertEquals("drain", result.getCurrentStep());
        assertEquals("Active", result.getFeatureState());
        assertEquals("KSM_NATIVE", result.getFeatureStateSource());
        assertEquals(java.util.Collections.singletonList("query"), result.getAllowedActions());
        assertEquals(sourceState, state.getState());
        assertEquals("Succeeded", state.getStatus());
        assertEquals("original", state.getReason());
        assertEquals("op", state.getControlOperationUuid());
        assertEquals(Long.valueOf(31L), state.getAppliedRevision());
        assertEquals("drain", state.getCurrentStep());
        assertEquals("Blocked", result.getStatus());

        MemoryStateInventory repeat = MemoryMetricsPresentation.inventory(state, null, 100100L);
        assertEquals(result.getStatus(), repeat.getStatus());
        assertEquals(result.getFeatureState(), repeat.getFeatureState());
        assertEquals(result.getAllowedActions(), repeat.getAllowedActions());
        assertEquals(result.getCurrentStep(), repeat.getCurrentStep());
    }

    @Test public void inventoryProjectsSevenValueStatusConservativelyFromPlanAndActivity() {
        assertProjectedStatus("Succeeded", "Blocked", null, "Blocked");
        assertProjectedStatus("Succeeded", "NeedsReview", null, "Blocked");
        assertProjectedStatus("Succeeded", "Blocked", "active-task", "Unknown");
        assertProjectedStatus("Failed", "Blocked", "active-task", "Unknown");
        assertProjectedStatus("Blocked", "Blocked", "active-task", "Blocked");
        assertProjectedStatus("Failed", "Blocked", null, "Blocked");
        assertProjectedStatus("Blocked", "Blocked", null, "Blocked");
        assertProjectedStatus("Unknown", "Blocked", null, "Unknown");
        assertProjectedStatus("Succeeded", "Failed", null, "Failed");
        assertProjectedStatus("Succeeded", "Unknown", null, "Unknown");
        assertProjectedStatus("Failed", "Failed", null, "Failed");
        assertProjectedStatus("Failed", "Unknown", null, "Unknown");
        for (String raw : new String[]{"Queued", "Applying", "Draining"}) {
            assertProjectedStatus(raw, "Blocked", null, raw);
            assertProjectedStatus(raw, "Queued", null, raw);
            assertProjectedStatus(raw, "Unknown", null, raw);
        }
        for (String raw : new String[]{"Succeeded", "Failed", "Blocked"}) {
            for (String plan : new String[]{"Queued", "Applying", "Draining"}) {
                assertProjectedStatus(raw, plan, null, "Unknown");
            }
        }
        for (String raw : new String[]{null, "Cancelled", "Partial", "unexpected"}) {
            assertProjectedStatus(raw, "Blocked", null, "Unknown");
        }
        assertProjectedStatus("Failed", "Applied", null, "Failed");
        assertProjectedStatus("Unknown", "Applied", null, "Unknown");
    }

    @Test public void blockedProjectionDoesNotChangePausedResumeEligibilityOrSource() {
        String nativeState = "{\"managed\":true,\"ksmOnly\":false,\"bootId\":\"boot-1\","
                + "\"lastConfirmedOperationUuid\":\"op\",\"lastConfirmedAppliedRevision\":31,"
                + "\"poolGeneration\":\"gen\",\"lifecycle\":{\"activeState\":\"paused\",\"paused\":true},"
                + "\"host_zram\":{\"pool_generation\":\"gen\",\"device\":\"/dev/zram0\","
                + "\"quality\":\"native_host_device\",\"observed_at\":\"1970-01-01T00:01:40Z\"},"
                + "\"actual\":{\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":false,"
                + "\"pagesToScan\":2000,\"sleepMillis\":20}}}";
        MemoryStateVO blocked = state();
        blocked.setStatus("Succeeded"); blocked.setPolicyPlanStatus("Blocked");
        blocked.setControlOperationUuid("op"); blocked.setDesiredRevision(31); blocked.setAppliedRevision(31L);
        blocked.setState(nativeState);
        MemoryStateVO applied = state();
        applied.setStatus("Succeeded"); applied.setPolicyPlanStatus("Applied");
        applied.setControlOperationUuid("op"); applied.setDesiredRevision(31); applied.setAppliedRevision(31L);
        applied.setState(nativeState);

        MemoryStateInventory blockedResult = MemoryMetricsPresentation.inventory(blocked, null, 100100L, true, false);
        MemoryStateInventory appliedResult = MemoryMetricsPresentation.inventory(applied, null, 100100L, true, false);
        assertEquals("Blocked", blockedResult.getStatus());
        assertEquals("Succeeded", appliedResult.getStatus());
        assertEquals(appliedResult.getAllowedActions(), blockedResult.getAllowedActions());
        assertEquals(java.util.Arrays.asList("query", "drain", "resume"), blockedResult.getAllowedActions());
        assertEquals("Succeeded", blocked.getStatus());
        assertEquals("Blocked", blocked.getPolicyPlanStatus());
        assertEquals(nativeState, blocked.getState());
    }

    @Test public void uncertainRecoveryIsOfferedOnlyWithItsConfirmationRequest() {
        java.util.Map<String, String> request = java.util.Collections.singletonMap("expectedHostBootId", "boot");
        MemoryStateInventory offered = MemoryMetricsPresentation.inventory(state(), null, 100000L, false, false, request);
        MemoryStateInventory plain = MemoryMetricsPresentation.inventory(state(), null, 100000L, false, false, null);
        assertTrue(offered.getAllowedActions().contains("recoverUncertain"));
        assertEquals(request, offered.getUncertainRecovery());
        assertFalse(plain.getAllowedActions().contains("recoverUncertain"));
        assertNull(plain.getUncertainRecovery());
    }

    private void assertProjectedStatus(String raw, String plan, String active, String expected) {
        MemoryStateVO source = state();
        source.setStatus(raw); source.setPolicyPlanStatus(plan); source.setActiveTaskUuid(active);
        MemoryStateInventory result = MemoryMetricsPresentation.inventory(source, null, 100000L);
        assertEquals(expected, result.getStatus());
    }

    private MemoryStateVO state() {
        MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host");
        state.setStatus("Succeeded"); state.setLastSampleTime(100000L);
        state.setState("{\"lifecycle\":{\"paused\":true},\"savings\":{\"totalSavedEstimateBytes\":1234}}");
        return state;
    }
}
