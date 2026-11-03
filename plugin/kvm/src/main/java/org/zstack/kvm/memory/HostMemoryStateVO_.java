package org.zstack.kvm.memory;

import java.sql.Timestamp;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(HostMemoryStateVO.class)
public class HostMemoryStateVO_ {
    public static volatile SingularAttribute<HostMemoryStateVO, String> hostUuid;
    public static volatile SingularAttribute<HostMemoryStateVO, String> status;
    public static volatile SingularAttribute<HostMemoryStateVO, String> featureState;
    public static volatile SingularAttribute<HostMemoryStateVO, String> reason;
    public static volatile SingularAttribute<HostMemoryStateVO, String> allowedActions;
    public static volatile SingularAttribute<HostMemoryStateVO, String> activeJobUuid;
    public static volatile SingularAttribute<HostMemoryStateVO, Long> lastObservedAt;
    public static volatile SingularAttribute<HostMemoryStateVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostMemoryStateVO, Timestamp> lastOpDate;
}
