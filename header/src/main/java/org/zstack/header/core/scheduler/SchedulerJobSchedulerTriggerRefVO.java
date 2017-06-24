package org.zstack.header.core.scheduler;

import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */

@Entity
@Table
public class SchedulerJobSchedulerTriggerRefVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = SchedulerJobVO.class)
    private String schedulerJobUuid;

    @Column
    @ForeignKey(parentEntityClass = SchedulerTriggerVO.class)
    private String schedulerTriggerUuid;

    @Column
    private String jobGroup;

    @Column
    private String triggerGroup;

    @Column
    private String taskData;

    @Column
    private String taskClassName;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }

    public String getSchedulerTriggerUuid() {
        return schedulerTriggerUuid;
    }

    public void setSchedulerTriggerUuid(String schedulerTriggerUuid) {
        this.schedulerTriggerUuid = schedulerTriggerUuid;
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

    public String getTaskData() {
        return taskData;
    }

    public void setTaskData(String taskData) {
        this.taskData = taskData;
    }

    public String getTaskClassName() {
        return taskClassName;
    }

    public void setTaskClassName(String taskClassName) {
        this.taskClassName = taskClassName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
