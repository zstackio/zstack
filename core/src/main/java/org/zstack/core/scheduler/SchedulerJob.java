package org.zstack.core.scheduler;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    void run();
}
