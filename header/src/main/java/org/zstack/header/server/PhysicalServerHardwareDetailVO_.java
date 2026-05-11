package org.zstack.header.server;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalServerHardwareDetailVO.class)
public class PhysicalServerHardwareDetailVO_ {
    public static volatile SingularAttribute<PhysicalServerHardwareDetailVO, Long> id;
    public static volatile SingularAttribute<PhysicalServerHardwareDetailVO, String> serverUuid;
    public static volatile SingularAttribute<PhysicalServerHardwareDetailVO, String> type;
    public static volatile SingularAttribute<PhysicalServerHardwareDetailVO, String> itemModel;
    public static volatile SingularAttribute<PhysicalServerHardwareDetailVO, String> specification;
    public static volatile SingularAttribute<PhysicalServerHardwareDetailVO, String> firmwareVersion;
    public static volatile SingularAttribute<PhysicalServerHardwareDetailVO, String> healthStatus;
    public static volatile SingularAttribute<PhysicalServerHardwareDetailVO, String> extraInfo;
    public static volatile SingularAttribute<PhysicalServerHardwareDetailVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalServerHardwareDetailVO, Timestamp> lastOpDate;
}
