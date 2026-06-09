package org.zstack.compute.allocator

import org.junit.Test
import org.zstack.header.allocator.AbstractHostAllocatorFlow
import org.zstack.header.allocator.HostAllocatorSpec
import org.zstack.header.allocator.HostAllocatorTrigger
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostVO
import org.zstack.header.vm.VmInstanceInventory

class LastHostAllocatorFlowCase {
    @Test
    void testSelectLastHostWhenItIsNotSoftAvoided() {
        HostAllocatorSpec spec = spec("host2", ["host1"])
        List<HostVO> hosts = [host("host1"), host("host2"), host("host3")]

        assert hostUuids(allocate(spec, hosts)) == ["host2"]
    }

    @Test
    void testKeepCandidatesWhenLastHostIsSoftAvoided() {
        HostAllocatorSpec spec = spec("host2", ["host2"])
        List<HostVO> hosts = [host("host1"), host("host2"), host("host3")]

        assert hostUuids(allocate(spec, hosts)) == ["host1", "host2", "host3"]
    }

    @Test
    void testKeepCandidatesWhenLastHostIsMissing() {
        HostAllocatorSpec spec = spec("host4", [])
        List<HostVO> hosts = [host("host1"), host("host2"), host("host3")]

        assert hostUuids(allocate(spec, hosts)) == ["host1", "host2", "host3"]
    }

    private static List<HostVO> allocate(HostAllocatorSpec spec, List<HostVO> hosts) {
        LastHostAllocatorFlow flow = new LastHostAllocatorFlow()
        RecordingTrigger trigger = new RecordingTrigger()
        flow.setSpec(spec)
        flow.setCandidates(hosts)
        flow.setTrigger(trigger)

        flow.allocate()

        assert !trigger.skipCalled
        assert trigger.errorCode == null
        return trigger.nextCandidates
    }

    private static HostAllocatorSpec spec(String lastHostUuid, List<String> softAvoidHostUuids) {
        VmInstanceInventory vm = new VmInstanceInventory()
        vm.setLastHostUuid(lastHostUuid)

        HostAllocatorSpec spec = new HostAllocatorSpec()
        spec.setVmInstance(vm)
        spec.setSoftAvoidHostUuids(softAvoidHostUuids)
        return spec
    }

    private static HostVO host(String uuid) {
        HostVO host = new HostVO()
        host.setUuid(uuid)
        return host
    }

    private static List<String> hostUuids(List<HostVO> hosts) {
        return hosts.collect { it.uuid }
    }

    private static class RecordingTrigger implements HostAllocatorTrigger {
        List<HostVO> nextCandidates
        boolean skipCalled
        ErrorCode errorCode

        @Override
        void next(List<HostVO> candidates) {
            nextCandidates = candidates
        }

        @Override
        void skip() {
            skipCalled = true
        }

        @Override
        boolean isFirstFlow(AbstractHostAllocatorFlow flow) {
            return false
        }

        @Override
        void fail(ErrorCode errorCode) {
            this.errorCode = errorCode
        }
    }
}
