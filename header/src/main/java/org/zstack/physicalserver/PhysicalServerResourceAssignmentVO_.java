package org.zstack.physicalserver;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalServerResourceAssignmentVO.class)
public class PhysicalServerResourceAssignmentVO_ {
    public static volatile SingularAttribute<PhysicalServerResourceAssignmentVO, String> uuid;
    public static volatile SingularAttribute<PhysicalServerResourceAssignmentVO, String> serverUuid;
    public static volatile SingularAttribute<PhysicalServerResourceAssignmentVO, String> roleType;
    public static volatile SingularAttribute<PhysicalServerResourceAssignmentVO, String> cpuSet;
    public static volatile SingularAttribute<PhysicalServerResourceAssignmentVO, Long> memory;
    public static volatile SingularAttribute<PhysicalServerResourceAssignmentVO, PhysicalServerResourceAssignmentState> state;
    public static volatile SingularAttribute<PhysicalServerResourceAssignmentVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalServerResourceAssignmentVO, Timestamp> lastOpDate;
}
