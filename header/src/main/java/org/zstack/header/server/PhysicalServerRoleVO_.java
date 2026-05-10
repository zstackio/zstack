package org.zstack.header.server;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalServerRoleVO.class)
public class PhysicalServerRoleVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PhysicalServerRoleVO, String> serverUuid;
    public static volatile SingularAttribute<PhysicalServerRoleVO, String> roleType;
    public static volatile SingularAttribute<PhysicalServerRoleVO, String> roleUuid;
    public static volatile SingularAttribute<PhysicalServerRoleVO, SchedulingMode> schedulingMode;
    public static volatile SingularAttribute<PhysicalServerRoleVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalServerRoleVO, Timestamp> lastOpDate;
}
