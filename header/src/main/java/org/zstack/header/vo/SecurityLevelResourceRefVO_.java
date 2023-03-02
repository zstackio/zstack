package org.zstack.header.vo;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SecurityLevelResourceRefVO.class)
public class SecurityLevelResourceRefVO_ {
    public static volatile SingularAttribute<SecurityLevelResourceRefVO, String> resourceUuid;
    public static volatile SingularAttribute<SecurityLevelResourceRefVO, String> securityLevel;
}
