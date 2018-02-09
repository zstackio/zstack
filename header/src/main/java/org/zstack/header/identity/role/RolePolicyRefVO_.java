package org.zstack.header.identity.role;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RolePolicyRefVO.class)
public class RolePolicyRefVO_ {
    public static volatile SingularAttribute<RolePolicyRefVO, String> roleUuid;
    public static volatile SingularAttribute<RolePolicyRefVO, String> policyUuid;
    public static volatile SingularAttribute<RolePolicyRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<RolePolicyRefVO, Timestamp> lastOpDate;
}
