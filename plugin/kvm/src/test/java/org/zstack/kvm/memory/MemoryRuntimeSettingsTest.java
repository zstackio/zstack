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
            assertEquals(600, new APIQueryMemoryStateMsg().getLimit());
            assertEquals(600, new APIQueryMemoryTaskMsg().getLimit());
            APIQueryMemoryStateMsg request = new APIQueryMemoryStateMsg(); request.setLimit(700);
            assertEquals(700, request.getLimit());
            request.setLimit(801);
            try { request.getLimit(); fail("must reject configured page overflow"); }
            catch (MemoryOperationException expected) { assertNotNull(expected.getMessage()); }
        } finally { set(max, oldMax); set(def, oldDefault); }
    }
    @Test public void displayTtlCanBeConfiguredWithoutAffectingControl() throws Exception {
        GlobalConfig ttl = (GlobalConfig) settings().getField("DISPLAY_TTL_SECONDS").get(null);
        String old = ttl.value();
        try {
            set(ttl, "120");
            MemoryStateInventory state = new MemoryStateInventory(); state.setLastSampleTime(1000L);
            state.setState("{\"savings\":{\"quality\":\"Fresh\"}}");
            MemoryStatePresentation.populate(state, 91000); assertEquals("Fresh", state.getQuality());
            MemoryStatePresentation.populate(state, 121000); assertEquals("Stale", state.getQuality());
        } finally { set(ttl, old); }
    }
}
