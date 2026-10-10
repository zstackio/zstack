package org.zstack.kvm.memory;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryPolicyAdmissionRulesTest {
    private MemoryStateVO state(long sample, String capabilities) {
        MemoryStateVO state = new MemoryStateVO(); state.setLastSampleTime(sample); state.setCapabilities(capabilities);
        return state;
    }

    @Test public void evaluatesOnlyChangedFieldsAndMapsIndependentMechanisms() {
        Map<String, String> changed = MemoryPolicyAdmissionRules.changedFields(
                "{\"ksm\":{\"enabled\":false},\"zram\":{\"enabled\":false}}",
                "{\"ksm\":{\"enabled\":true},\"zram\":{\"enabled\":false}}");
        Map<String, String> blocked = MemoryPolicyAdmissionRules.blockedFields(changed,
                state(1000, "{\"supported\":true,\"ksm\":true,\"zram\":false}"), 2000);
        assertTrue("unchanged inherited ZRAM cannot block KSM", blocked.isEmpty());

        changed = MemoryPolicyAdmissionRules.changedFields(
                "{\"ksm\":{\"zeroPagesEnabled\":false}}",
                "{\"ksm\":{\"zeroPagesEnabled\":true}}");
        blocked = MemoryPolicyAdmissionRules.blockedFields(changed,
                state(1000, "{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":false,\"ksmZeroPagesReasonCode\":\"ZERO_PAGE_COUNTER_UNAVAILABLE\"}"), 2000);
        assertEquals("ZERO_PAGE_COUNTER_UNAVAILABLE", blocked.get("ksm.zeroPagesEnabled"));
    }

    @Test public void missingAndStaleSamplesHaveDifferentReasonsButExplicitDisableIsSafe() {
        Map<String, String> enable = MemoryPolicyAdmissionRules.changedFields("{}", "{\"zram\":{\"enabled\":true}}");
        assertEquals("CAPABILITY_UNKNOWN", MemoryPolicyAdmissionRules.blockedFields(enable, null, 1000).get("zram.enabled"));
        assertEquals("CAPABILITY_STALE", MemoryPolicyAdmissionRules.blockedFields(enable,
                state(1000, "{\"supported\":true,\"zram\":true}"), 1000000).get("zram.enabled"));

        Map<String, String> disable = MemoryPolicyAdmissionRules.changedFields(
                "{\"ksm\":{\"enabled\":true},\"zram\":{\"enabled\":true}}",
                "{\"ksm\":{\"enabled\":false},\"zram\":{\"enabled\":false}}");
        assertTrue(MemoryPolicyAdmissionRules.blockedFields(disable, null, 1000).isEmpty());
        Map<String, String> drainAndTune = MemoryPolicyAdmissionRules.changedFields(
                "{\"zram\":{\"enabled\":true,\"logicalCapacityBytes\":1024}}",
                "{\"zram\":{\"enabled\":false,\"logicalCapacityBytes\":2048}}");
        assertTrue("disabling while editing a retained capacity must remain safe without a current capability sample",
                MemoryPolicyAdmissionRules.blockedFields(drainAndTune, null, 1000).isEmpty());
    }

    @Test public void overallUnsupportedTargetCannotBeBypassedByInconsistentFeatureFlag() {
        Map<String, String> changed = new LinkedHashMap<>(); changed.put("ksm.enabled", "true");
        MemoryStateVO state = state(1000, "{\"supported\":false,\"reasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\",\"ksm\":true}");
        assertEquals("UNSUPPORTED_RECLAIM_KERNEL", MemoryPolicyAdmissionRules.blockedFields(changed, state, 2000).get("ksm.enabled"));
    }

    @Test public void trueToFalseBooleanIsNotMistakenForUnsupportedEnable() {
        Map<String, String> changed = MemoryPolicyAdmissionRules.changedFields(
                "{\"writeback\":{\"enabled\":true}}", "{\"writeback\":{\"enabled\":false}}");
        assertTrue(MemoryPolicyAdmissionRules.blockedFields(changed, null, 1000).isEmpty());
    }

    @Test public void enabledZramRemovedFromSparseTargetIsNotTreatedAsSafeDisable() {
        Map<String, String> removed = MemoryPolicyAdmissionRules.changedFields(
                "{\"zram\":{\"enabled\":true}}", "{\"schemaVersion\":1}");
        assertEquals("null", removed.get("zram.enabled"));
        MemoryStateVO unsupported = state(1000,
                "{\"supported\":true,\"zram\":false,\"zramReasonCode\":\"ABI_INCOMPLETE\"}");
        assertEquals("ABI_INCOMPLETE", MemoryPolicyAdmissionRules.blockedFields(
                removed, unsupported, 2000).get("zram.enabled"));

        Map<String, String> explicitlyEnabled = MemoryPolicyAdmissionRules.changedFields(
                "{}", "{\"zram\":{\"enabled\":true}}");
        assertEquals("CAPABILITY_UNKNOWN", MemoryPolicyAdmissionRules.blockedFields(
                explicitlyEnabled, null, 2000).get("zram.enabled"));
    }
}
