package org.zstack.header.identity.role;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SystemRoleVO.class)
public class SystemRoleVO_ extends RoleVO_ {
    public static SingularAttribute<SystemRoleVO, SystemRoleType> systemRoleType;
}
