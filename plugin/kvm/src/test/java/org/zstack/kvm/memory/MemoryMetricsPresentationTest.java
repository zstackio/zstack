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

    private MemoryStateVO state() {
        MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host");
        state.setStatus("Succeeded"); state.setLastSampleTime(100000L);
        state.setState("{\"lifecycle\":{\"paused\":true},\"savings\":{\"totalSavedEstimateBytes\":1234}}");
        return state;
    }
}
