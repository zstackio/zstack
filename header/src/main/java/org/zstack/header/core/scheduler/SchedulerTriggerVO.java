package org.zstack.header.core.scheduler;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */

@Entity
@Table
public class SchedulerTriggerVO extends ResourceVO{

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String schedulerType;

    @Column
    private Integer schedulerInterval;

    @Column
    private Integer repeatCount;

    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String managementNodeUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp startTime;

    @Column
    private Timestamp stopTime;

    @Column
    private Timestamp lastOpDate;

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getStopTime() {
        return stopTime;
    }

    public void setStopTime(Timestamp stopTime) {
        this.stopTime = stopTime;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSchedulerType() {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    public Integer getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(Integer schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }
}
