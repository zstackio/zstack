package org.zstack.scheduler;

import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/8.
 */
public class SchedulerTask {
    private String type;
    private String jobUuid;
    private String triggerUuid;
    private Integer taskRepeatCount;
    private Integer taskInterval;
    private String targetResourceUuid;
    private String jobClassName;
    private String jobData;
    private String cron;
    private Timestamp startTime;
    private Timestamp stopTime;
    private Timestamp createTime;

    public Timestamp getStopTime() {
        return stopTime;
    }

    public void setStopTime(Timestamp stopTime) {
        this.stopTime = stopTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public String getTriggerUuid() {
        return triggerUuid;
    }

    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }

    public Integer getTaskRepeatCount() {
        return taskRepeatCount;
    }

    public void setTaskRepeatCount(Integer taskRepeatCount) {
        this.taskRepeatCount = taskRepeatCount;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getJobClassName() {
        return jobClassName;
    }

    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public Integer getTaskInterval() {
        return taskInterval;
    }

    public void setTaskInterval(Integer taskInterval) {
        this.taskInterval = taskInterval;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }
}
