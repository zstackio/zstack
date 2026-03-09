package org.zstack.header.identity;

import org.zstack.header.vo.ResourceVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(UserGroupVO.class)
public class UserGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<UserGroupVO, String> name;
    public static volatile SingularAttribute<UserGroupVO, String> description;
    public static volatile SingularAttribute<UserGroupVO, String> accountUuid;
    public static volatile SingularAttribute<UserGroupVO, Timestamp> createDate;
}
