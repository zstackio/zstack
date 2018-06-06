package org.zstack.header.identity.role;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RoleUserGroupRefVO.class)
public class RoleUserGroupRefVO_ {
    public static volatile SingularAttribute<RoleUserGroupRefVO, String> roleUuid;
    public static volatile SingularAttribute<RoleUserGroupRefVO, String> groupUuid;
    public static volatile SingularAttribute<RoleUserGroupRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<RoleUserGroupRefVO, Timestamp> lastOpDate;
}
