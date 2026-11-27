package org.zstack.kvm.memory;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.*;

public class MemoryStandardConfigAdapterTest {
    @Test public void resettingAGlobalSettingToItsDefaultIsAlwaysAllowed() throws Exception {
        MemoryStandardField field = MemoryStandardField.forPath("zram.logicalCapacityBytes");
        Field defaultValue = org.zstack.core.config.GlobalConfig.class.getDeclaredField("defaultValue");
        defaultValue.setAccessible(true);
        Object previous = defaultValue.get(field.config());
        defaultValue.set(field.config(), "");
        try {
            // ResetGlobalConfig writes the blank default; validating it would stop the reset half way.
            assertTrue(MemoryStandardConfigurationCoordinator.restoresDefault(field, ""));
            assertFalse(MemoryStandardConfigurationCoordinator.restoresDefault(field, "8589934592"));
        } finally {
            defaultValue.set(field.config(), previous);
        }
    }

    @Test public void thePreparedBackendIsRecordedNotConfigured() {
        // Only a successful PrepareHostMemoryWritebackBackend records the backend the Host really claimed.
        for (String path : Arrays.asList("writeback.backendResourceUuid", "writeback.backendCapacityBytes")) {
            assertThrows(org.zstack.core.config.GlobalConfigException.class,
                    () -> MemoryStandardConfigurationCoordinator.requireConfigurable(MemoryStandardField.forPath(path)));
        }
        for (String path : Arrays.asList("writeback.enabled", "zram.enabled", "zram.logicalCapacityBytes")) {
            MemoryStandardConfigurationCoordinator.requireConfigurable(MemoryStandardField.forPath(path));
        }
    }

    @Test public void typedBlankWritesRejectButFeatureNoneIsAccepted() {
        MemoryStandardField field = MemoryStandardField.forPath("ksm.enabled");
        assertThrows(org.zstack.core.config.GlobalConfigException.class,
                () -> MemoryStandardConfigurationCoordinator.validateScalar(field, " \t "));
        assertNull(MemoryStandardConfigCodec.decode("ksm.enabled", "   "));
        for (String path : Arrays.asList("ksm.enabled", "ksm.zeroPagesEnabled", "zram.enabled", "writeback.enabled")) {
            try { MemoryStandardConfigurationCoordinator.validateScalar(MemoryStandardField.forPath(path), "none"); }
            catch (RuntimeException invalid) { fail(path + " must accept the existing none encoding: " + invalid.getMessage()); }
            assertNull(MemoryStandardConfigCodec.decode(path, "none"));
        }
        assertThrows(IllegalArgumentException.class,
                () -> MemoryStandardConfigCodec.decode("zram.logicalCapacityBytes", "none"));
    }
    @Test public void hotMemoryIntervalIsSecondsWithIndependentBounds() {
        MemoryStandardField field = MemoryStandardField.forPath("hotMemory.interval");
        for (String value : Arrays.asList("300", "301", "600", "86400")) {
            MemoryStandardConfigurationCoordinator.validateScalar(field, value);
            assertEquals(Long.valueOf(value), MemoryStandardConfigCodec.decode(field.path(), value));
        }
        for (String value : Arrays.asList("0", "299", "86401", "-1", "600.5", "none", "true")) {
            assertThrows(org.zstack.core.config.GlobalConfigException.class,
                    () -> MemoryStandardConfigurationCoordinator.validateScalar(field, value));
        }
        // Adding a seconds field must not weaken existing byte-count validation.
        assertThrows(org.zstack.core.config.GlobalConfigException.class,
                () -> MemoryStandardConfigurationCoordinator.validateScalar(
                        MemoryStandardField.ZRAM_RAM_LIMIT, "600"));
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.decode("hotMemory.enabled", "true"));
        assertEquals(Boolean.FALSE, MemoryStandardConfigCodec.decode("hotMemory.enabled", "false"));
        assertNull(MemoryStandardConfigCodec.decode("hotMemory.enabled", "none"));
    }

@Test public void aRamLimitAboveTheZramLogicalCapacityIsRefused() {
        long memory = 16464789504L;
        // The default logical capacity is half of MemTotal, page aligned, as the service resolves it.
        long half = 8232394752L;
        assertNotNull(MemoryStandardConfigurationCoordinator.capacityRefusal(null, 8769265664L, memory));
        assertNull(MemoryStandardConfigurationCoordinator.capacityRefusal(null, half, memory));
        assertNotNull(MemoryStandardConfigurationCoordinator.capacityRefusal(null, half + 4096, memory));
        assertNotNull(MemoryStandardConfigurationCoordinator.capacityRefusal(4L << 30, 5L << 30, memory));
        assertNull(MemoryStandardConfigurationCoordinator.capacityRefusal(12L << 30, 10L << 30, memory));
        // A RAM limit left to the service default follows the logical capacity.
        assertNull(MemoryStandardConfigurationCoordinator.capacityRefusal(4L << 30, null, memory));
        // Without the Host's memory the default cannot be judged here; the service still refuses it.
        assertNull(MemoryStandardConfigurationCoordinator.capacityRefusal(null, 8769265664L, null));
        String reason = MemoryStandardConfigurationCoordinator.capacityRefusal(null, 8769265664L, memory);
        assertTrue(reason, reason.contains("8769265664") && reason.contains("8232394752"));
    }
}
