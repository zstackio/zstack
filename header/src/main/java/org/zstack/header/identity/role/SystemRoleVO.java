package org.zstack.header.identity.role;

import javax.persistence.*;

@Table
@Entity
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
public class SystemRoleVO extends RoleVO {
    @Column
    @Enumerated(EnumType.STRING)
    private SystemRoleType systemRoleType;

    public SystemRoleType getSystemRoleType() {
        return systemRoleType;
    }

    public void setSystemRoleType(SystemRoleType systemRoleType) {
        this.systemRoleType = systemRoleType;
    }
}
