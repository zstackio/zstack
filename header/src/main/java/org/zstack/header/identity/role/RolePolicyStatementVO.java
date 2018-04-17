package org.zstack.header.identity.role;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table
public class RolePolicyStatementVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String statement;

    @Column
    @ForeignKey(parentEntityClass = RoleVO.class, parentKey = "uuid")
    private String roleUuid;

    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public RolePolicyStatementVO copy(String roleUuid) {
        RolePolicyStatementVO vo = new RolePolicyStatementVO();
        vo.uuid = UUID.randomUUID().toString().replace("-", "");
        vo.statement = statement;
        vo.roleUuid = roleUuid;
        return vo;
    }

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

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
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
