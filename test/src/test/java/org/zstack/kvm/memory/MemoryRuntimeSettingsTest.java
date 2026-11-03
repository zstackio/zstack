package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.core.config.GlobalConfig;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

public class MemoryRuntimeSettingsTest {
    private Class<?> settings() {
        try { return Class.forName("org.zstack.kvm.memory.MemoryOptimizationGlobalConfig"); }
        catch (ClassNotFoundException e) { fail("Runtime limits must be exposed as GlobalConfig"); return null; }
    }
    private void set(GlobalConfig config, String value) throws Exception {
        Field field = GlobalConfig.class.getDeclaredField("value"); field.setAccessible(true); field.set(config, value);
    }
    @Test public void everyHostIsObservedWithinTheDisplaySampleLifetime() {
        // 100 Hosts every 30 seconds left the Hosts beyond 300 per management node Unknown.
        assertEquals(100, MemoryOptimizationGlobalConfig.observationBatch(40));
        assertEquals(500, MemoryOptimizationGlobalConfig.observationBatch(1000));
        assertTrue(2 * 30 * 1000L < MemoryOptimizationGlobalConfig.displayTtlMillis());
    }
    @Test public void displayTtlCanBeConfiguredWithoutAffectingControl() throws Exception {
        GlobalConfig ttl = (GlobalConfig) settings().getField("DISPLAY_TTL_SECONDS").get(null);
        String old = ttl.value();
        try {
            set(ttl, "120");
            java.util.Map<String, Object> savings = new java.util.HashMap<>();
            savings.put("quality", "Fresh"); savings.put("sampleTime", 1000L);
            assertEquals("Fresh", MemoryMetricsPresentation.quality(savings, 91000));
            assertEquals("Stale", MemoryMetricsPresentation.quality(savings, 121000));
        } finally { set(ttl, old); }
    }
}
