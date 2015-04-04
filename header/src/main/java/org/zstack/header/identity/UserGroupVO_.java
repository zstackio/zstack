package org.zstack.header.identity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(UserGroupVO.class)
public class UserGroupVO_ {
    public static volatile SingularAttribute<UserGroupVO, String> uuid;
    public static volatile SingularAttribute<UserGroupVO, String> name;
    public static volatile SingularAttribute<UserGroupVO, String> description;
    public static volatile SingularAttribute<UserGroupVO, String> accountUuid;
    public static volatile SingularAttribute<UserGroupVO, Timestamp> createDate;
}
