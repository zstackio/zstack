package org.zstack.network.securitygroup;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmNicSecurityGroupRefVO.class)
public class VmNicSecurityGroupRefVO_ {
    public static volatile SingularAttribute<VmNicSecurityGroupRefVO_, String> uuid;
    public static volatile SingularAttribute<VmNicSecurityGroupRefVO_, String> vmNicUuid;
    public static volatile SingularAttribute<VmNicSecurityGroupRefVO_, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmNicSecurityGroupRefVO_, String> securityGroupUuid;
    public static volatile SingularAttribute<VmNicSecurityGroupRefVO_, Integer> priority;
    public static volatile SingularAttribute<VmNicSecurityGroupRefVO_, Timestamp> createDate;
    public static volatile SingularAttribute<VmNicSecurityGroupRefVO_, Timestamp> lastOpDate;
}
