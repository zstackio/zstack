package org.zstack.header.server;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Index rationale (2026-04-20):
 * - UNIQUE(serverUuid, roleType): enforces the PS → role business rule (one role of each type per
 *   physical server), and lets queries starting from serverUuid (PS → role lookup during
 *   mutual-exclusion checks, capacity roll-up) use the leading column.
 * - INDEX(roleUuid, roleType): required by the {@code HostCapacityVO} VIEW's
 *   {@code LEFT JOIN PhysicalServerRoleVO r ON r.roleUuid = h.uuid AND r.roleType = 'KVM_HOST'}
 *   (capacity PRD §2.1). Without it every HostVO EAGER load triggers a full RoleVO scan.
 */
@Entity
@Table(name = "PhysicalServerRoleVO",
        uniqueConstraints = @UniqueConstraint(columnNames = {"serverUuid", "roleType"}),
        indexes = @javax.persistence.Index(name = "idx_role_uuid_type", columnList = "roleUuid, roleType"))
public class PhysicalServerRoleVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = PhysicalServerVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String serverUuid;

    @Column
    private String roleType;

    @Column
    private String roleUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private SchedulingMode schedulingMode;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public SchedulingMode getSchedulingMode() {
        return schedulingMode;
    }

    public void setSchedulingMode(SchedulingMode schedulingMode) {
        this.schedulingMode = schedulingMode;
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
