package org.zstack.kvm.memory;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MemoryLifecycleHooksTest {
    @Test
    public void ksmOnlyStateDoesNotCreateMigrationPoolHold() {
        assertFalse(MemoryLifecycleHooks.hasPoolGeneration("{\"ksm\":{\"pagesSharing\":100}}"));
        assertFalse(MemoryLifecycleHooks.hasPoolGeneration("{\"managed\":true,\"zram\":{\"enabled\":false}}"));
    }

    @Test
    public void vmAccountingCounterIsNotAPoolIdentity() {
        assertFalse(MemoryLifecycleHooks.hasPoolGeneration(
                "{\"host_zram\":{\"pool_generation\":\"\"},\"vm_accounting\":{\"inventory\":{\"pool_generation\":0}}}"));
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

    @Test public void aRoundOfAgentCallsReportsItsFailuresOnceWhenTheLastOneReturns() {
        List<String> errors = new ArrayList<>();
        MemoryLifecycleHooks.Round round = new MemoryLifecycleHooks.Round("Host[host-a] VM bindings", 3, errors::add);
        round.finished("vm-1", null);
        round.finished("vm-2", "AGENT_UNREACHABLE");
        assertTrue("the round is not over yet", errors.isEmpty());
        round.finished("vm-3", "VM_NOT_FOUND");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0), errors.get(0).contains("vm-2") && errors.get(0).contains("vm-3") && !errors.get(0).contains("vm-1"));
        List<String> quiet = new ArrayList<>();
        new MemoryLifecycleHooks.Round("Host[host-a] VM bindings", 1, quiet::add).finished("vm-1", null);
        assertTrue(quiet.isEmpty());
    }
}
