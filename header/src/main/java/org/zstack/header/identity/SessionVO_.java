package org.zstack.header.identity;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SessionVO.class)
public class SessionVO_ {
    public static volatile SingularAttribute<SessionVO, String> uuid;
    public static volatile SingularAttribute<SessionVO, String> accountUuid;
    public static volatile SingularAttribute<SessionVO, String> userUuid;
    public static volatile SingularAttribute<SessionVO, Timestamp> expiredDate;
    public static volatile SingularAttribute<SessionVO, Timestamp> createDate;
}
