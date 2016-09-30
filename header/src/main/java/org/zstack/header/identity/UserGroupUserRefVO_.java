package org.zstack.header.identity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/9/2015.
 */
@StaticMetamodel(UserGroupUserRefVO.class)
public class UserGroupUserRefVO_ {
    public static volatile SingularAttribute<UserGroupVO, Long> id;
    public static volatile SingularAttribute<UserGroupVO, String> userUuid;
    public static volatile SingularAttribute<UserGroupVO, String> groupUuid;
}
