package org.zstack.header.identity.role;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(RolePolicyStatementVO.class)
public class RolePolicyStatementVO_ {
    public static volatile SingularAttribute<RolePolicyStatementVO, String> uuid;
    public static volatile SingularAttribute<RolePolicyStatementVO, String> roleUuid;
    public static volatile SingularAttribute<RolePolicyStatementVO, String> statement;
}
