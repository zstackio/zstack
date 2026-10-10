package org.zstack.test.unittest.kvm;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.config.GlobalConfig;
import org.zstack.header.allocator.ReservedHostCapacity;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KvmHostReserveExtension;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.data.SizeUnit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class TestKvmHostReservedCapacityCase {
    private KvmHostReserveExtension extension;
    private ResourceConfigFacade resourceConfigFacade;

    @Before
    public void setUp() throws Exception {
        extension = new KvmHostReserveExtension();
        resourceConfigFacade = mock(ResourceConfigFacade.class);
        Field field = KvmHostReserveExtension.class.getDeclaredField("rcf");
        field.setAccessible(true);
        field.set(extension, resourceConfigFacade);

        Map<String, String> memoryValues = new HashMap<>();
        memoryValues.put("host-a", "1G");
        memoryValues.put("host-b", "2G");
        mockBatchLookup(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY, memoryValues);

        Map<String, String> cpuValues = new HashMap<>();
        cpuValues.put("host-a", "2");
        cpuValues.put("host-b", "0");
        mockBatchLookup(KVMGlobalConfig.RESERVED_CPU_CAPACITY, cpuValues);
    }

    @Test
    public void testBatchLookupPreservesHostReservationsAndInput() {
        List<String> hostUuids = new ArrayList<>(Arrays.asList("host-a", "host-b"));

        Map<String, ReservedHostCapacity> results = extension.getReservedHostsCapacity(hostUuids);

        assertEquals(2, results.size());
        assertEquals(SizeUnit.GIGABYTE.toByte(1), results.get("host-a").getReservedMemoryCapacity());
        assertEquals(2L, results.get("host-a").getReservedCpuCapacity());
        assertEquals(SizeUnit.GIGABYTE.toByte(2), results.get("host-b").getReservedMemoryCapacity());
        assertEquals(0L, results.get("host-b").getReservedCpuCapacity());
        assertEquals(Arrays.asList("host-a", "host-b"), hostUuids);
    }

    @Test
    public void testBatchLookupAcceptsImmutableHostList() {
        Map<String, ReservedHostCapacity> results = extension.getReservedHostsCapacity(
                Collections.singletonList("host-a"));

        assertEquals(1, results.size());
        assertEquals(SizeUnit.GIGABYTE.toByte(1), results.get("host-a").getReservedMemoryCapacity());
        assertEquals(2L, results.get("host-a").getReservedCpuCapacity());
    }

    private void mockBatchLookup(GlobalConfig config, Map<String, String> values) {
        doAnswer(invocation -> {
            List<String> resourceUuids = invocation.getArgument(1);
            Map<String, String> results = new HashMap<>();
            for (String uuid : resourceUuids) {
                if (values.containsKey(uuid)) {
                    results.put(uuid, values.get(uuid));
                }
            }
            // ResourceConfig removes resolved resource UUIDs from its input list.
            resourceUuids.removeAll(results.keySet());
            return results;
        }).when(resourceConfigFacade).getResourceConfigValues(eq(config), anyList(), eq(String.class));
    }
}
