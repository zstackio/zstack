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
    @Test public void configurablePaginationIsNotA500ItemProductCap() throws Exception {
        Class<?> type = settings();
        GlobalConfig max = (GlobalConfig) type.getField("QUERY_MAX_PAGE_SIZE").get(null);
        GlobalConfig def = (GlobalConfig) type.getField("QUERY_DEFAULT_PAGE_SIZE").get(null);
        String oldMax = max.value(), oldDefault = def.value();
        try {
            set(max, "800"); set(def, "600");
            assertEquals(600, MemoryOptimizationGlobalConfig.pageSize(null));
            assertEquals(700, MemoryOptimizationGlobalConfig.pageSize(700));
            try { MemoryOptimizationGlobalConfig.pageSize(801); fail("must reject configured page overflow"); }
            catch (MemoryOperationException expected) { assertNotNull(expected.getMessage()); }
        } finally { set(max, oldMax); set(def, oldDefault); }
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
