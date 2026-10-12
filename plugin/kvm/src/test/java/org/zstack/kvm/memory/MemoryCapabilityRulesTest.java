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

    @Test public void freshExplicitPartialNativeKsmCanExposeOnlyItsIndependentCapability() {
        MemoryStateVO state = new MemoryStateVO();
        state.setStatus("Unknown");
        state.setLastSampleTime(null); // no complete sample has been observed
        state.setCapabilities("{\"nativeKsmPartial\":{\"observationKind\":\"KSM_NATIVE_PARTIAL\","
                + "\"capabilities\":{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":false},"
                + "\"sampleTime\":1500,"
                + "\"bootId\":\"boot-a\",\"ksm\":{\"enabled\":true,"
                + "\"pagesToScan\":1250,\"sleepMillis\":10}}}");
        state.setState("{\"nativeKsmObservation\":{\"bootId\":\"boot-a\",\"sampleTime\":1500}}");
        Map<String, MemoryFieldCapability> fields = fields();
        MemoryCapabilityRules.restrict(fields, state, 2000);

        assertTrue("fresh native KSM evidence can inform KSM field capability while full state stays Unknown",
                fields.get("ksm.enabled").writable);
        assertFalse(fields.get("ksm.zeroPagesEnabled").writable);
        assertFalse(fields.get("zram.enabled").writable);
        assertFalse(fields.get("writeback.enabled").writable);
        assertEquals("Unknown", state.getStatus());
        assertNull("partial evidence never fabricates a complete sample time", state.getLastSampleTime());
    }

    @Test public void partialKsmCapabilityRequiresExplicitFreshMarker() {
        MemoryStateVO state = new MemoryStateVO(); state.setStatus("Unknown");
        state.setCapabilities("{\"supported\":true,\"ksm\":true,\"nativeKsmPartial\":{"
                + "\"observationKind\":\"KSM_NATIVE_PARTIAL\",\"sampleTime\":1500,\"bootId\":\"boot-a\"}}");
        Map<String, MemoryFieldCapability> fields = fields();
        MemoryCapabilityRules.restrict(fields, state, 2000);
        assertFalse(fields.get("ksm.enabled").writable);

        state.setCapabilities("{\"nativeKsmPartial\":{\"capabilities\":{\"supported\":true,"
                + "\"ksm\":true,\"ksmZeroPages\":true},\"observationKind\":\"KSM_NATIVE_PARTIAL\","
                + "\"sampleTime\":1000,\"bootId\":\"boot-a\",\"ksm\":{\"enabled\":true,"
                + "\"pagesToScan\":1,\"sleepMillis\":1}}}");
        state.setState("{\"nativeKsmObservation\":{\"bootId\":\"boot-a\",\"sampleTime\":1000}}");
        fields = fields();
        MemoryCapabilityRules.restrict(fields, state, 1000 + MemoryOptimizationGlobalConfig.displayTtlMillis());
        assertFalse("exact TTL boundary is stale", fields.get("ksm.enabled").writable);
    }

}
