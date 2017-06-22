package org.zstack.scheduler;

import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by AlanJager on 2017/6/10.
 */
public interface SchedulerType {
    String VM = VmInstanceVO.class.getSimpleName();
    String START_VM = "startVm";
    String STOP_VM = "stopVm";
    String REBOOT_VM = "rebootVm";

    String SNAP_SHOT = VolumeSnapshotVO.class.getSimpleName();
    String VOLUME_SNAPSHOT = "volumeSnapshot";
}
