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
}
