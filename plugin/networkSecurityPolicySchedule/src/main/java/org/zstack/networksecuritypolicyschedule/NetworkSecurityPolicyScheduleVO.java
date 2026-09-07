package org.zstack.networksecuritypolicyschedule;

import org.zstack.header.vo.BaseResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

@BaseResource
@Entity
@Table
public class NetworkSecurityPolicyScheduleVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String resourceType;

    @Column
    private String resourceUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private NetworkSecurityPolicyScheduleTimeType timeType;

    @Column
    @Enumerated(EnumType.STRING)
    private NetworkSecurityPolicyScheduleRepeatType repeatType;

    @Column
    private Date startDate;

    @Column
    private Date endDate;

    @Column
    private Time startTime;

    @Column
    private Time endTime;

    @Column
    private String weekDays;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public NetworkSecurityPolicyScheduleTimeType getTimeType() {
        return timeType;
    }

    public void setTimeType(NetworkSecurityPolicyScheduleTimeType timeType) {
        this.timeType = timeType;
    }

    public NetworkSecurityPolicyScheduleRepeatType getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(NetworkSecurityPolicyScheduleRepeatType repeatType) {
        this.repeatType = repeatType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    public String getWeekDays() {
        return weekDays;
    }

    public void setWeekDays(String weekDays) {
        this.weekDays = weekDays;
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
