package org.zstack.scheduler;

import org.zstack.header.Component;
import org.zstack.header.core.scheduler.SchedulerVO;

/**
 * Created by Mei Lei on 6/22/16.
 */
public interface SchedulerFacade extends Component {
    boolean runScheduler(SchedulerTask job);
    void pauseSchedulerJob(String uuid);
    void resumeSchedulerJob(String uuid);
    void deleteSchedulerJobByResourceUuid(String uuid);
}
