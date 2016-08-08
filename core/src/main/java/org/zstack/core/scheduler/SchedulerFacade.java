package org.zstack.core.scheduler;

import org.zstack.header.Component;

/**
 * Created by Mei Lei on 6/22/16.
 */
public interface SchedulerFacade extends Component {
    void runScheduler(SchedulerJob job);
}
