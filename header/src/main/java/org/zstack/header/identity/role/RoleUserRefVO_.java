package org.zstack.header.identity.role;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RoleUserRefVO.class)
public class RoleUserRefVO_ {
    public static volatile SingularAttribute<RoleUserRefVO, String> roleUuid;
    public static volatile SingularAttribute<RoleUserRefVO, String> userUuid;
    public static volatile SingularAttribute<RoleUserRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<RoleUserRefVO, Timestamp> lastOpDate;
}
