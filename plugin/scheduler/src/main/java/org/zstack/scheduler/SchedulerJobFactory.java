package org.zstack.scheduler;


/**
 * Created by AlanJager on 2017/6/9.
 */
public interface SchedulerJobFactory {
    SchedulerJob createSchedulerJob(APICreateSchedulerJobMsg msg);
}
