package org.zstack.header.identity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/9/2015.
 */
@StaticMetamodel(UserPolicyRefVO.class)
public class UserPolicyRefVO_ {
    public static volatile SingularAttribute<UserPolicyRefVO, Long> id;
    public static volatile SingularAttribute<UserPolicyRefVO, String> policyUuid;
    public static volatile SingularAttribute<UserPolicyRefVO, String> userUuid;
}
