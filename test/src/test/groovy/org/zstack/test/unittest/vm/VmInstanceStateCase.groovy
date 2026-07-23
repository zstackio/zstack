package org.zstack.test.unittest.vm

import org.junit.Test
import org.zstack.header.vm.VmAbnormalLifeCycleStruct
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceStateEvent

class VmInstanceStateCase {
    @Test
    void testPausedCanEnterVolumeRecovering() {
        VmInstanceState actual = VmInstanceState.Paused.nextState(VmInstanceStateEvent.volumeRecovering)

        assert actual == VmInstanceState.VolumeRecovering :
                "paused VM cannot enter volume recovery: expected=${VmInstanceState.VolumeRecovering}, actual=${actual}"
    }

    @Test
    void testPausedHostStateKeepsVolumeRecoveringBusinessState() {
        VmInstanceState actual = VmInstanceState.VolumeRecovering.nextState(VmInstanceStateEvent.paused)

        assert actual == VmInstanceState.VolumeRecovering :
                "host paused state breaks volume recovery: expected=${VmInstanceState.VolumeRecovering}, actual=${actual}"
    }

    @Test
    void testPausedVolumeRecoveringAbnormalLifecycleOperation() {
        String hostUuid = "host-uuid"
        VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation actual =
                VmAbnormalLifeCycleStruct.getVmAbnormalLifeCycleOperation(
                        hostUuid,
                        VmInstanceState.VolumeRecovering,
                        VmInstanceState.Paused,
                        hostUuid,
                        hostUuid)

        assert actual?.name() == "VmPausedFromVolumeRecoveringStateHostNotChanged" :
                "paused recovery VM is not recognized on the same host: " +
                        "expected=VmPausedFromVolumeRecoveringStateHostNotChanged, actual=${actual}"

        actual = VmAbnormalLifeCycleStruct.getVmAbnormalLifeCycleOperation(
                hostUuid,
                VmInstanceState.VolumeRecovering,
                VmInstanceState.Paused,
                hostUuid,
                "another-host-uuid")

        assert actual == null :
                "paused recovery VM on a different host must not match the host-unchanged operation: expected=null, actual=${actual}"
    }
}
