package org.zstack.header.identity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(UserVO.class)
public class UserVO_ {
    public static volatile SingularAttribute<UserVO, String> uuid;
    public static volatile SingularAttribute<UserVO, String> name;
    public static volatile SingularAttribute<UserVO, String> password;
    public static volatile SingularAttribute<UserVO, Timestamp> createDate;
    public static volatile SingularAttribute<UserVO, String> accountUuid;
    public static volatile SingularAttribute<UserVO, String> securityKey;
    public static volatile SingularAttribute<UserVO, String> description;
    public static volatile SingularAttribute<UserVO, String> token;
    public static volatile SingularAttribute<UserVO, Timestamp> lastOpDate;
}
