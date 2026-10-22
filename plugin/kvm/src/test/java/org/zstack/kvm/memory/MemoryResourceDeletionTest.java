package org.zstack.kvm.memory;

import org.junit.Test;
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
    @Test public void hostDeletionIsNeverBlockedByMemoryState() throws Exception {
        hooks(null).preDeleteHost(host());
        hooks(sample("{\"managed\":false}")).preDeleteHost(host());
        MemoryStateVO busy = sample("{\"managed\":true,\"poolGeneration\":\"g1\"}");
        busy.setActiveTaskUuid("unknown-task"); busy.setLastSampleTime(null);
        hooks(busy).preDeleteHost(host());
    }
}
