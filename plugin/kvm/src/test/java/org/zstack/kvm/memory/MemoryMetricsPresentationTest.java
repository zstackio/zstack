package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryMetricsPresentationTest {
    @Test public void prometheusFailureNeverFallsBackToCachedAgentSavings() {
        MemoryStateVO state = new MemoryStateVO();
        state.setHostUuid("host"); state.setLastSampleTime(100000L);
        state.setState("{\"savings\":{\"totalSavedEstimateBytes\":1234,\"quality\":\"Fresh\",\"sampleTime\":99000}}");
        assertEquals("Unknown", MemoryMetricsPresentation.quality(null, 100000));
        assertTrue(MemoryMetricsPresentation.metrics(null, 100000).isEmpty());
        MemoryStateVO copy = MemoryMetricsPresentation.withSavings(state, null);
        assertFalse(copy.getState().contains("1234"));
        assertTrue("the persisted observation is unchanged", state.getState().contains("1234"));
    }

    @Test public void originalMetricTimeSurvivesFreshControlPoll() {
        Map<String, Object> savings = new HashMap<>();
        savings.put("sampleTime", 10000L); savings.put("quality", "Fresh");
        savings.put("totalSavedEstimateBytes", -20L);
        assertEquals("Stale", MemoryMetricsPresentation.quality(savings, 100000));
        Map<String, Object> metrics = MemoryMetricsPresentation.metrics(savings, 100000);
        assertEquals(-20, ((Number) metrics.get("totalSavedEstimateBytes")).longValue());
        assertEquals(100000L, metrics.get("presentationTime"));
        assertEquals(MemoryOptimizationGlobalConfig.displayTtlMillis(), metrics.get("displayTtlMillis"));
        savings.put("sampleTime", 99000L);
        assertEquals("Fresh", MemoryMetricsPresentation.quality(savings, 100000));
        savings.put("sampleTime", 106000L);
        assertEquals("a sample from the future is not trusted", "Unknown", MemoryMetricsPresentation.quality(savings, 100000));
    }
}
