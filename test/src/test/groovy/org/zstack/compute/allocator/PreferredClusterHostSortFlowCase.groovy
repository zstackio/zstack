package org.zstack.compute.allocator

import org.junit.Test
import org.zstack.header.allocator.HostAllocatorSpec
import org.zstack.header.host.HostInventory

class PreferredClusterHostSortFlowCase {
    @Test
    void testNoPreferClusterDoesNotChangeHostOrder() {
        HostAllocatorSpec spec = new HostAllocatorSpec()
        spec.setSoftAvoidHostUuids(["host1"])
        List<HostInventory> hosts = [
                host("host1", "cluster1"),
                host("host2", "cluster2")
        ]

        PreferredClusterHostSortFlow flow = sort(spec, hosts)

        assert hostUuids(hosts) == ["host1", "host2"]
        assert hostUuids(flow.subCandidates) == ["host1", "host2"]
        assert !flow.skipNext()
    }

    @Test
    void testMissingPreferClusterDoesNotChangeHostOrder() {
        HostAllocatorSpec spec = new HostAllocatorSpec()
        spec.setPreferClusterUuid("cluster3")
        List<HostInventory> hosts = [
                host("host1", "cluster1"),
                host("host2", "cluster2")
        ]

        PreferredClusterHostSortFlow flow = sort(spec, hosts)

        assert hostUuids(hosts) == ["host1", "host2"]
        assert hostUuids(flow.subCandidates) == ["host1", "host2"]
        assert !flow.skipNext()
    }

    @Test
    void testPreferClusterSortsHostOrder() {
        HostAllocatorSpec spec = new HostAllocatorSpec()
        spec.setPreferClusterUuid("cluster1")
        List<HostInventory> hosts = [
                host("host1", "cluster2"),
                host("host2", "cluster1"),
                host("host3", "cluster1")
        ]

        PreferredClusterHostSortFlow flow = sort(spec, hosts)

        assert hostUuids(hosts) == ["host2", "host3", "host1"]
        assert hostUuids(flow.subCandidates) == ["host2", "host3", "host1"]
        assert !flow.skipNext()
    }

    @Test
    void testPreferClusterKeepsOriginalOrderWithinEachGroup() {
        HostAllocatorSpec spec = new HostAllocatorSpec()
        spec.setPreferClusterUuid("cluster1")
        List<HostInventory> hosts = [
                host("host1", "cluster2"),
                host("host2", "cluster1"),
                host("host3", "cluster2"),
                host("host4", "cluster1")
        ]

        PreferredClusterHostSortFlow flow = sort(spec, hosts)

        assert hostUuids(hosts) == ["host2", "host4", "host1", "host3"]
        assert hostUuids(flow.subCandidates) == ["host2", "host4", "host1", "host3"]
        assert !flow.skipNext()
    }

    @Test
    void testPreferClusterKeepsSoftAvoidHostsAfterFallbackHosts() {
        HostAllocatorSpec spec = new HostAllocatorSpec()
        spec.setPreferClusterUuid("cluster1")
        spec.setSoftAvoidHostUuids(["host1", "host4"])
        List<HostInventory> hosts = [
                host("host1", "cluster1"),
                host("host2", "cluster2"),
                host("host3", "cluster1"),
                host("host4", "cluster2")
        ]

        PreferredClusterHostSortFlow flow = sort(spec, hosts)

        assert hostUuids(hosts) == ["host3", "host2", "host1", "host4"]
        assert hostUuids(flow.subCandidates) == ["host3", "host2", "host1", "host4"]
        assert !flow.skipNext()
    }

    private static PreferredClusterHostSortFlow sort(HostAllocatorSpec spec, List<HostInventory> hosts) {
        PreferredClusterHostSortFlow flow = new PreferredClusterHostSortFlow()
        flow.setSpec(spec)
        flow.setCandidates(hosts)
        flow.sort()
        return flow
    }

    private static HostInventory host(String uuid, String clusterUuid) {
        HostInventory host = new HostInventory()
        host.setUuid(uuid)
        host.setClusterUuid(clusterUuid)
        return host
    }

    private static List<String> hostUuids(List<HostInventory> hosts) {
        return hosts.collect { it.uuid }
    }
}
