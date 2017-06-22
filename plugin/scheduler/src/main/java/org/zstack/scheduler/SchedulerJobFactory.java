package org.zstack.scheduler;

import org.zstack.header.core.scheduler.SchedulerJobInventory;
import org.zstack.header.core.scheduler.SchedulerJobVO;

/**
 * Created by AlanJager on 2017/6/9.
 */
public interface SchedulerJobFactory {
    SchedulerJob createSchedulerJob(APICreateSchedulerJobMsg msg);

//    SchedulerJob getSchedulerJob(SchedulerJobVO vo);
//
//    String getSchedulerJobType();
//
//    SchedulerJobInventory getSchedulerJobInventory(String uuid);
}
