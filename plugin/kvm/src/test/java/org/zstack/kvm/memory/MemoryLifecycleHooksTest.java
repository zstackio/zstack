package org.zstack.kvm.memory;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MemoryLifecycleHooksTest {
    @Test
    public void ksmOnlyStateDoesNotCreateMigrationPoolHold() {
        assertFalse(MemoryLifecycleHooks.hasPoolGeneration("{\"ksm\":{\"pagesSharing\":100}}"));
        assertFalse(MemoryLifecycleHooks.hasPoolGeneration("{\"managed\":true,\"zram\":{\"enabled\":false}}"));
    }

    @Test
    public void nestedZramPoolGenerationIsRecognized() {
        assertTrue(MemoryLifecycleHooks.hasPoolGeneration(
                "{\"host_zram\":{\"pool_generation\":\"pool-1\"}}"));
        assertTrue(MemoryLifecycleHooks.hasPoolGeneration(
                "{\"zram\":{\"poolGeneration\":\"pool-2\"}}"));
    }

    @Test
    public void vmGenerationSupportsAgentNamingAndMissingIdentityFailsClosed() {
        MemoryAgentResponse response = new MemoryAgentResponse();
        Map<String, Object> vm = new HashMap<>();
        vm.put("instance_generation", "instance-7");
        response.state = Collections.singletonMap("vms", Collections.singletonMap("vm-1", vm));
        assertEquals("instance-7", MemoryLifecycleHooks.instanceGeneration(response, "vm-1"));
        assertNull(MemoryLifecycleHooks.instanceGeneration(response, "vm-2"));
    }
}
