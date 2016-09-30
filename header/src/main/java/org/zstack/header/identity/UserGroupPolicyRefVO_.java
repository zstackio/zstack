package org.zstack.header.identity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/9/2015.
 */
@StaticMetamodel(UserGroupPolicyRefVO.class)
public class UserGroupPolicyRefVO_ {
    public static volatile SingularAttribute<UserGroupPolicyRefVO, Long> id;
    public static volatile SingularAttribute<UserGroupPolicyRefVO, String> policyUuid;
    public static volatile SingularAttribute<UserGroupPolicyRefVO, String> groupUuid;
}
