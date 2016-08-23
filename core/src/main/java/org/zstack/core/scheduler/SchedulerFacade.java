package org.zstack.core.scheduler;

import org.zstack.header.Component;
import org.zstack.header.core.scheduler.SchedulerVO;

/**
 * Created by Mei Lei on 6/22/16.
 */
public interface SchedulerFacade extends Component {
    SchedulerVO runScheduler(SchedulerJob job);
    void pauseSchedulerJob(String uuid);
    void resumeSchedulerJob(String uuid);
    void deleteSchedulerJob(String uuid);
}
