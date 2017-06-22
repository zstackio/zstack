//package org.zstack.scheduler;
//
//import org.zstack.header.core.scheduler.SchedulerJobInventory;
//import org.zstack.header.core.scheduler.SchedulerJobVO;
//import org.zstack.header.exception.CloudRuntimeException;
//import org.zstack.scheduler.storage.volume.snapshot.CreateVolumeSnapshotJob;
//
///**
// * Created by AlanJager on 2017/6/12.
// */
//public class VolumeSnapshotSchedulerJobFactory implements SchedulerJobFactory {
//    @Override
//    public SchedulerJob createSchedulerJob(APICreateSchedulerJobMsg msg) {
//        if (msg.getType().equals(SchedulerType.VOLUME_SNAPSHOT)) {
//            CreateVolumeSnapshotJob job = new CreateVolumeSnapshotJob();
//
//            return job;
//        } else {
//            return new CreateVolumeSnapshotJob();
//        }
//
//    }
//
//    @Override
//    public SchedulerJob getSchedulerJob(SchedulerJobVO vo) {
//        return null;
//    }
//
//    @Override
//    public String getSchedulerJobType() {
//        return SchedulerType.SNAP_SHOT;
//    }
//
//    @Override
//    public SchedulerJobInventory getSchedulerJobInventory(String uuid) {
//        return null;
//    }
//}
