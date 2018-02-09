package org.zstack.header.identity.role;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Table
@Entity
@IdClass(RoleUserGroupRefVO.CompositeID.class)
public class RoleUserGroupRefVO {
    static class CompositeID implements Serializable {
        private String roleUuid;
        private String groupUuid;

        public String getRoleUuid() {
            return roleUuid;
        }

        public void setRoleUuid(String roleUuid) {
            this.roleUuid = roleUuid;
        }

        public String getGroupUuid() {
            return groupUuid;
        }

        public void setGroupUuid(String groupUuid) {
            this.groupUuid = groupUuid;
        }
    }

    @Id
    @Column
    private String roleUuid;
    @Id
    @Column
    private String groupUuid;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
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
