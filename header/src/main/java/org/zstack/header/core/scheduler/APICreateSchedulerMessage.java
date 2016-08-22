package org.zstack.header.core.scheduler;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;

/**
 * Created by root on 8/3/16.
 */
public class APICreateSchedulerMessage extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String schedulerName;
    @APIParam(maxLength = 2048, required = false)
    private String schedulerDescription;
    @APIParam(validValues = {"simple", "cron"})
    private String type;
    @APIParam(required = false)
    private Integer interval;
    @APIParam(required = false)
    private Integer repeatCount;
    @APIParam(required = false)
    private Long startTime;
    @APIParam(required = false)
    private String cron;

    @APINoSee
    private String jobName;
    @APINoSee
    private String jobGroup;
    @APINoSee
    private String triggerGroup;
    @APINoSee
    private String triggerName;

    public String getSchedulerDescription() {
        return schedulerDescription;
    }

    public void setSchedulerDescription(String schedulerDescription) {
        this.schedulerDescription = schedulerDescription;
    }

    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getTriggerGroup() {
        return triggerGroup;
    }

    public void setTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }
}
