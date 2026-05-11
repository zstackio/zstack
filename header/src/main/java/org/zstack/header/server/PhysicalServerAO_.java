package org.zstack.header.server;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalServerAO.class)
public class PhysicalServerAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PhysicalServerAO, String> zoneUuid;
    public static volatile SingularAttribute<PhysicalServerAO, String> poolUuid;
    public static volatile SingularAttribute<PhysicalServerAO, String> name;
    public static volatile SingularAttribute<PhysicalServerAO, String> description;
    public static volatile SingularAttribute<PhysicalServerAO, String> managementIp;
    public static volatile SingularAttribute<PhysicalServerAO, String> architecture;
    public static volatile SingularAttribute<PhysicalServerAO, String> serialNumber;
    public static volatile SingularAttribute<PhysicalServerAO, String> manufacturer;
    public static volatile SingularAttribute<PhysicalServerAO, String> model;
    public static volatile SingularAttribute<PhysicalServerAO, PhysicalServerState> state;
    public static volatile SingularAttribute<PhysicalServerAO, PhysicalServerPowerStatus> powerStatus;
    public static volatile SingularAttribute<PhysicalServerAO, String> oobManagementType;
    public static volatile SingularAttribute<PhysicalServerAO, String> oobAddress;
    public static volatile SingularAttribute<PhysicalServerAO, Integer> oobPort;
    public static volatile SingularAttribute<PhysicalServerAO, String> oobUsername;
    public static volatile SingularAttribute<PhysicalServerAO, String> oobPassword;
    public static volatile SingularAttribute<PhysicalServerAO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalServerAO, Timestamp> lastOpDate;
}
