package org.zstack.core.scheduler;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by Mei Lei on 7/11/16.
 */
public interface SchedulerJob {
    Date getStartDate();
    Timestamp getCreateDate();
    int getSchedulerInterval();
    int getRepeat();
    String getSchedulerName();
    String getJobName();
    String getJobGroup();
    String getTriggerName();
    String getTriggerGroup();
    String getType();
    String getCron();
    String getResourceUuid();
    void run();
}
