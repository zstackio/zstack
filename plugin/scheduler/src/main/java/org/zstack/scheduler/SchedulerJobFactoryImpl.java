package org.zstack.scheduler;

import org.zstack.header.core.scheduler.SchedulerJobInventory;
import org.zstack.header.core.scheduler.SchedulerJobVO;
import org.zstack.scheduler.storage.volume.snapshot.CreateVolumeSnapshotJob;
import org.zstack.scheduler.vm.RebootVmInstanceJob;
import org.zstack.scheduler.vm.StartVmInstanceJob;
import org.zstack.scheduler.vm.StopVmInstanceJob;

/**
 * Created by AlanJager on 2017/6/12.
 */
public class SchedulerJobFactoryImpl implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(APICreateSchedulerJobMsg msg) {
        if (msg.getType().equals(SchedulerType.START_VM)) {
            StartVmInstanceJob job = new StartVmInstanceJob(msg);
            job.setVmUuid(msg.getTargetResourceUuid());
            job.setTargetResourceUuid(msg.getTargetResourceUuid());

            return job;
        } else if (msg.getType().equals(SchedulerType.STOP_VM)) {
            StopVmInstanceJob job = new StopVmInstanceJob(msg);
            job.setVmUuid(msg.getTargetResourceUuid());
            job.setTargetResourceUuid(msg.getTargetResourceUuid());

            return job;
        } else if (msg.getType().equals(SchedulerType.REBOOT_VM)) {
            RebootVmInstanceJob job = new RebootVmInstanceJob(msg);
            job.setVmUuid(msg.getTargetResourceUuid());
            job.setTargetResourceUuid(msg.getTargetResourceUuid());

            return job;
        } else if (msg.getType().equals(SchedulerType.VOLUME_SNAPSHOT)){
            CreateVolumeSnapshotJob job = new CreateVolumeSnapshotJob(msg);
            job.setVolumeUuid(msg.getTargetResourceUuid());

            return job;
        } else {
            return null;
        }

    }
}
