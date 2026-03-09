package org.zstack.header.identity.role;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SystemRoleVO.class)
public class SystemRoleVO_ extends RoleVO_ {
    public static SingularAttribute<SystemRoleVO, SystemRoleType> systemRoleType;
}
