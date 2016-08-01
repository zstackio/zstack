package org.zstack.core.scheduler;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by Mei Lei on 7/11/16.
 */
@Entity
@Table
@Inheritance(strategy= InheritanceType.JOINED)
public class SchedulerVO {
    @Id
    @Column
    private String uuid;
    @Column
    private String schedulerName;
    @Column
    private String schedulerType;
    @Column
    private int schedulerInterval;
    @Column
    private int repeatCount;
    @Column
    private String cronScheduler;
    @Column
    private String jobName;
    @Column
    private String jobGroup;
    @Column
    private String triggerName;
    @Column
    private String triggerGroup;
    @Column
    private String managementNodeUuid;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp startDate;
    @Column
    private Timestamp lastOpDate;
    /**
     * @desc jobClassName define the job
     */
    @Column
    private String jobClassName;
    @Column
    private String jobData;
    @Column
    private String status;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public int getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(int schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
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

    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public String getTriggerGroup() {
        return triggerGroup;
    }

    public void setTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
    }

    public String getJobClassName() {
        return jobClassName;
    }

    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }
    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public String getSchedulerType() {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getCronScheduler() {
        return cronScheduler;
    }

    public void setCronScheduler(String cronScheduler) {
        this.cronScheduler = cronScheduler;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
