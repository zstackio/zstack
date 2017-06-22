//package org.zstack.scheduler;
//
//import org.zstack.header.core.scheduler.SchedulerJobInventory;
//import org.zstack.header.core.scheduler.SchedulerJobVO;
//import org.zstack.scheduler.vm.StartVmInstanceJob;
//
///**
// * Created by AlanJager on 2017/6/9.
// */
//public class VmInstanceSchedulerJobFactory implements SchedulerJobFactory {
//    @Override
//    public SchedulerJob createSchedulerJob(APICreateSchedulerJobMsg msg) {
//        if (msg.getType().equals(SchedulerType.START_VM)) {
//            StartVmInstanceJob job = new StartVmInstanceJob();
//            job.setVmUuid(msg.getTargetResourceUuid());
//            job.setName(msg.getName());
//            job.setDescription(msg.getDescription());
//            job.setTargetResourceUuid(msg.getTargetResourceUuid());
//
//            return job;
//        } else {
//            return new StartVmInstanceJob();
//        }
//    }
//
//    @Override
//    public SchedulerJob getSchedulerJob(SchedulerJobVO vo) {
//        return null;
//    }
//
//    @Override
//    public String getSchedulerJobType() {
//        return SchedulerType.VM;
//    }
//
//
//    @Override
//    public SchedulerJobInventory getSchedulerJobInventory(String uuid) {
//        return null;
//    }
//}
