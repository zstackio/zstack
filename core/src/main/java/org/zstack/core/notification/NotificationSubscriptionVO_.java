package org.zstack.core.notification;

/**
 * Created by xing5 on 2017/3/15.
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(NotificationSubscriptionVO.class)
public class NotificationSubscriptionVO_ {
    public static volatile SingularAttribute<NotificationSubscriptionVO, String> uuid;
    public static volatile SingularAttribute<NotificationSubscriptionVO, String> name;
    public static volatile SingularAttribute<NotificationSubscriptionVO, String> description;
    public static volatile SingularAttribute<NotificationSubscriptionVO, String> notificationName;
    public static volatile SingularAttribute<NotificationSubscriptionVO, String> filter;
}
