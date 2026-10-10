package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class MemoryPreviewRulesTest {
    private MemoryPolicyInventory inventory(String scope) {
        MemoryPolicyInventory inventory = new MemoryPolicyInventory();
        inventory.setScope(scope);
        inventory.setPolicy("{\"zram\":{\"enabled\":true,\"logicalCapacityBytes\":8589934592}}");
        inventory.setEffectivePolicy(inventory.getPolicy());
        return inventory;
    }

    @Test public void resolvesMissingHostCapacityWithoutOverwritingExplicitValue() {
        MemoryPolicyInventory inventory = inventory("Host");
        assertTrue(MemoryPreviewRules.needsCapacity(inventory));
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("logicalCapacityBytes", 16L << 30);
        values.put("ramLimitBytes", 4L << 30);
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.state = Collections.singletonMap("policyDefaults", Collections.singletonMap("zram", values));
        MemoryPreviewRules.resolveCapacity(inventory, response);
        MemoryPolicyConfig result = MemoryPolicyRules.decode(inventory.getEffectivePolicy());
        assertEquals(Long.valueOf(8L << 30), result.zram.logicalCapacityBytes);
        assertEquals(Long.valueOf(4L << 30), result.zram.ramLimitBytes);
        assertFalse(MemoryPreviewRules.needsCapacity(inventory));
        // Defaults from one Host must never become global defaults.
        assertFalse(MemoryPreviewRules.needsCapacity(inventory("Global")));
    }

    @Test public void missingOrInvalidReadbackNeverInventsCapacity() {
        MemoryPolicyInventory inventory = inventory("Host");
        String before = inventory.getPolicy();
        for (MemoryAgentResponse response : new MemoryAgentResponse[] {null, new MemoryAgentResponse()}) {
            try { MemoryPreviewRules.resolveCapacity(inventory, response); fail(); }
            catch (IllegalArgumentException expected) { assertEquals(before, inventory.getPolicy()); }
        }
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.state = Collections.singletonMap("policyDefaults", Collections.singletonMap("zram",
                Collections.singletonMap("ramLimitBytes", 16L << 30)));
        try { MemoryPreviewRules.resolveCapacity(inventory, response); fail(); }
        catch (IllegalArgumentException expected) { assertEquals(before, inventory.getPolicy()); }
    }

    @Test public void successfulEnvelopeWithAbsentOrNullRequiredDefaultIsRejectedAtomically() {
        for (boolean explicitNull : new boolean[] {false, true}) {
            MemoryPolicyInventory inventory = inventory("Host");
            String own = inventory.getPolicy();
            String effective = inventory.getEffectivePolicy();
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("logicalCapacityBytes", 16L << 30);
            if (explicitNull) defaults.put("ramLimitBytes", null);
            MemoryAgentResponse response = new MemoryAgentResponse();
            response.state = Collections.singletonMap("policyDefaults", Collections.singletonMap("zram", defaults));
            try {
                MemoryPreviewRules.resolveCapacity(inventory, response);
                fail("successful envelope does not prove a missing default is known");
            } catch (IllegalArgumentException expected) {
                assertEquals("CAPACITY_DEFAULT_UNKNOWN", expected.getMessage());
                assertEquals(own, inventory.getPolicy());
                assertEquals(effective, inventory.getEffectivePolicy());
                assertTrue(MemoryPreviewRules.needsCapacity(inventory));
            }
        }
    }

    @Test public void oneResolvedDefaultDoesNotPartiallyMutatePolicyWhenTheOtherIsMissing() {
        MemoryPolicyInventory inventory = inventory("Host");
        inventory.setPolicy("{\"zram\":{\"enabled\":true}}");
        inventory.setEffectivePolicy(inventory.getPolicy());
        String before = inventory.getPolicy();
        for (String present : new String[] {"logicalCapacityBytes", "ramLimitBytes"}) {
            MemoryAgentResponse response = new MemoryAgentResponse();
            response.state = Collections.singletonMap("policyDefaults", Collections.singletonMap("zram",
                    Collections.singletonMap(present, 4L << 30)));
            try { MemoryPreviewRules.resolveCapacity(inventory, response); fail("other default is missing"); }
            catch (IllegalArgumentException expected) {
                assertEquals("CAPACITY_DEFAULT_UNKNOWN", expected.getMessage());
                assertEquals(before, inventory.getPolicy());
                assertEquals(before, inventory.getEffectivePolicy());
            }
        }
    }

    @Test public void responseOnlyNeedsTheDefaultNotAlreadyExplicitInPolicy() {
        MemoryPolicyInventory inventory = inventory("Host");
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.state = Collections.singletonMap("policyDefaults", Collections.singletonMap("zram",
                Collections.singletonMap("ramLimitBytes", 4L << 30)));
        MemoryPreviewRules.resolveCapacity(inventory, response);
        assertEquals(Long.valueOf(8L << 30), MemoryPolicyRules.decode(inventory.getEffectivePolicy()).zram.logicalCapacityBytes);
        assertEquals(Long.valueOf(4L << 30), MemoryPolicyRules.decode(inventory.getEffectivePolicy()).zram.ramLimitBytes);
        assertFalse(MemoryPreviewRules.needsCapacity(inventory));
    }
}
