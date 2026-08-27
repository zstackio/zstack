package org.zstack.physicalserver;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.sql.Timestamp;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "ukPhysicalServerResourceAssignment",
        columnNames = {"serverUuid", "roleType"}
))
public class PhysicalServerResourceAssignmentVO {
    @Id
    @Column
    @Index
    private String uuid;

    @Column
    @Index
    @ForeignKey(parentEntityClass = PhysicalServerVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String serverUuid;

    @Column
    private String roleType;

    @Column(nullable = false, length = 4096)
    private String cpuSet;

    @Column
    private Long memory;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PhysicalServerResourceAssignmentState state;

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

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getCpuSet() {
        return cpuSet;
    }

    public void setCpuSet(String cpuSet) {
        this.cpuSet = cpuSet;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public PhysicalServerResourceAssignmentState getState() {
        return state;
    }

    public void setState(PhysicalServerResourceAssignmentState state) {
        this.state = state;
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
