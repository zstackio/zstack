package org.zstack.scheduler;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by Mei Lei on 7/11/16.
 */
public interface SchedulerJob {
    Timestamp getCreateDate();
    String getJobName();
    String getResourceUuid();
    String getTargetResourceUuid();
    void run();
}
