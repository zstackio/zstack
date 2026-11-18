package org.zstack.physicalserver;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalServerVO.class)
public class PhysicalServerVO_ {
    public static volatile SingularAttribute<PhysicalServerVO, String> uuid;
    public static volatile SingularAttribute<PhysicalServerVO, String> zoneUuid;
    public static volatile SingularAttribute<PhysicalServerVO, String> serialNumber;
    public static volatile SingularAttribute<PhysicalServerVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalServerVO, Timestamp> lastOpDate;
}
