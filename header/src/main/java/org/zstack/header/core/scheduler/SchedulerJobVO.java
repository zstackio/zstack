package org.zstack.header.core.scheduler;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceAttributes;
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
@ResourceAttributes(nameField = "jobName")
public class SchedulerJobVO extends ResourceVO {
    @Column
    private String targetResourceUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String jobName;

    @Column
    private String jobData;

    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String managementNodeUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
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

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
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

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
