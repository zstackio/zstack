package org.zstack.network.securitygroup;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SecurityGroupVO.class)
public class SecurityGroupVO_ {
    public static volatile SingularAttribute<SecurityGroupVO, String> uuid;
    public static volatile SingularAttribute<SecurityGroupVO, String> name;
    public static volatile SingularAttribute<SecurityGroupVO, String> description;
    public static volatile SingularAttribute<SecurityGroupVO, SecurityGroupState> state;
    public static volatile SingularAttribute<SecurityGroupVO, Long> internalId;
    public static volatile SingularAttribute<SecurityGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<SecurityGroupVO, Timestamp> lastOpDate;
}
