package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class MemoryResourceDeletionTest {
    private MemoryLifecycleHooks hooks(MemoryStateVO state) throws Exception {
        MemoryLifecycleHooks result = new MemoryLifecycleHooks();
        Field field = MemoryLifecycleHooks.class.getDeclaredField("repository"); field.setAccessible(true);
        field.set(result, new MemoryRepository() {
            @Override public List<MemoryStateVO> states(List<String> hosts, int start, int limit) {
                return state == null ? Collections.emptyList() : Collections.singletonList(state);
            }
        });
        return result;
    }
    private HostInventory host() { HostInventory h = new HostInventory(); h.setUuid("host1"); return h; }
    private MemoryStateVO sample(String raw) {
        MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host1"); state.setStatus("Succeeded");
        state.setLastSampleTime(System.currentTimeMillis()); state.setState(raw); return state;
    }
    @Test public void unresolvedOperationBlocksDeletionEvenBeforeManagedAck() throws Exception {
        MemoryStateVO state = sample("{\"managed\":false}");
        state.setStatus("Unknown"); state.setActiveTaskUuid("unknown-operation");
        try { hooks(state).preDeleteHost(host()); fail("Missing managed ACK is not proof no operation ran"); }
        catch (HostException expected) { assertTrue(expected.getMessage().contains("MEMORY_DRAIN_REQUIRED")); }
    }
    @Test public void futureSafeSampleIsNotValidRemovalProof() throws Exception {
        MemoryStateVO state = sample("{\"managed\":true,\"safeToRemove\":true}");
        state.setLastSampleTime(System.currentTimeMillis() + 60000);
        try { hooks(state).preDeleteHost(host()); fail("Future-dated data is not fresh proof"); }
        catch (HostException expected) { assertTrue(expected.getMessage().contains("MEMORY_DRAIN_REQUIRED")); }
    }
    @Test public void knownUnmanagedOrFreshDrainedHostsKeepExistingDeletionPath() throws Exception {
        hooks(null).preDeleteHost(host());
        hooks(sample("{\"managed\":false}")).preDeleteHost(host());
        hooks(sample("{\"managed\":true,\"safeToRemove\":true}")).preDeleteHost(host());
    }
    @Test public void corruptedOwnedStateCannotBypassDrain() throws Exception {
        MemoryStateVO state = sample("not json"); state.setDesiredRevision(1); state.setControlOperationUuid("last-operation");
        try { hooks(state).preDeleteHost(host()); fail("Owned state must fail closed"); }
        catch (HostException expected) { assertTrue(expected.getMessage().contains("MEMORY_DRAIN_REQUIRED")); }
    }
}
