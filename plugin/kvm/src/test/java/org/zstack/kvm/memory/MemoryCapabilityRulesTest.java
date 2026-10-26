package org.zstack.kvm.memory;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryCapabilityRulesTest {
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
