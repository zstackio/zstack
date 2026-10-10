package org.zstack.kvm.memory;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryCapabilityRulesTest {
    private Map<String, MemoryFieldCapability> fields() {
        Map<String, MemoryFieldCapability> fields = new LinkedHashMap<>();
        for (String field : new String[]{"ksm.enabled", "ksm.zeroPagesEnabled", "zram.enabled", "writeback.enabled"})
            fields.put(field, new MemoryFieldCapability(true, true, "boolean", null, "NextPolicyCycle"));
        return fields;
    }
    @Test public void perMechanismCapabilityDoesNotBecomeGlobalSupport() {
        MemoryStateVO state = new MemoryStateVO(); state.setLastSampleTime(1000L);
        state.setCapabilities("{\"supported\":true,\"reasonCode\":\"SUPPORTED\",\"ksm\":true,\"ksmZeroPages\":false,\"zram\":false}");
        Map<String, MemoryFieldCapability> fields = fields();
        MemoryCapabilityRules.restrict(fields, state, 2000);
        assertTrue(fields.get("ksm.enabled").writable);
        assertFalse(fields.get("ksm.zeroPagesEnabled").writable);
        assertFalse(fields.get("zram.enabled").writable);
        assertFalse(fields.get("writeback.enabled").writable);
    }
    @Test public void staleOrUnsupportedHostCannotEnableOptimization() {
        MemoryStateVO state = new MemoryStateVO(); state.setLastSampleTime(1000L);
        state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":true}");
        Map<String, MemoryFieldCapability> fields = fields();
        MemoryCapabilityRules.restrict(fields, state, 100000);
        assertFalse(fields.get("ksm.enabled").writable);
        state.setLastSampleTime(99999L);
        state.setCapabilities("{\"supported\":false,\"reasonCode\":\"UNSUPPORTED_OS_RELEASE\"}");
        MemoryCapabilityRules.restrict(fields, state, 100000);
        assertEquals("UNSUPPORTED_OS_RELEASE", fields.get("zram.enabled").reasonCode);
    }
    @Test public void exactDisplayExpiryIsAlreadyStale() {
        MemoryStateVO state = new MemoryStateVO(); state.setLastSampleTime(1000L);
        state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":true}");
        Map<String, MemoryFieldCapability> fields = fields();
        MemoryCapabilityRules.restrict(fields, state, 91000);
        assertFalse(fields.get("ksm.enabled").writable);
    }

}
