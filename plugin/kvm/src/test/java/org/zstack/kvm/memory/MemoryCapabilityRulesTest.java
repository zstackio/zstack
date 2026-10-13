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
        assertTrue("writeback intent remains writable when execution capability is unavailable",
                fields.get("writeback.enabled").writable);
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
        assertTrue(fields.get("writeback.enabled").writable);
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


    @Test public void coldAdmissionTimeRemainsAnIntegerInTheHostHttpCommand() {
        long sample = 1791787546218L;
        MemoryStateVO state = new MemoryStateVO(); state.setStatus("Unknown");
        String partial = "{\"observationKind\":\"KSM_NATIVE_PARTIAL\",\"sampleTime\":" + sample
                + ",\"bootId\":\"boot-a\",\"coldKsmAdmissionReady\":true,"
                + "\"coldKsmAdmissionReason\":\"LOCAL_CONTROL_FENCE_CLEAR\","
                + "\"capabilities\":{\"supported\":true,\"ksm\":true},"
                + "\"ksm\":{\"enabled\":true,\"pagesToScan\":100,\"sleepMillis\":20}}";
        state.setCapabilities("{\"nativeKsmPartial\":" + partial + "}");
        state.setState("{\"nativeKsmObservation\":" + partial + "}");
        Map<String, Object> observation = MemoryCapabilityRules.coldKsmObservation(state, sample + 10);
        assertNotNull(observation);
        assertEquals(Long.class, observation.get("sampleTime").getClass());
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("ksmNativeObservationSampleTime", observation.get("sampleTime"));
        org.zstack.kvm.KVMHostHttpCallMsg msg = new org.zstack.kvm.KVMHostHttpCallMsg() { };
        msg.setCommand(command);
        assertEquals("{\"ksmNativeObservationSampleTime\":1791787546218}", msg.getCommand());
        Map<?, ?> decoded = org.zstack.utils.gson.JSONObjectUtil.toObject(msg.getCommand(), Map.class);
        assertEquals(sample, decoded.get("ksmNativeObservationSampleTime"));
        for (String bad : new String[]{"1791787546218.5", "true", "null", "\"1791787546218\""}) {
            state.setCapabilities("{\"nativeKsmPartial\":" + partial.replace(Long.toString(sample), bad) + "}");
            state.setState("{\"nativeKsmObservation\":" + partial.replace(Long.toString(sample), bad) + "}");
            assertNull(bad, MemoryCapabilityRules.coldKsmObservation(state, sample + 10));
        }
    }

}
