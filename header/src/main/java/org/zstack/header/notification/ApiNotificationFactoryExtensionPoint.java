package org.zstack.header.notification;

import java.util.Map;

/**
 * Created by xing5 on 2017/3/18.
 */
public interface ApiNotificationFactoryExtensionPoint {
    Map<Class, ApiNotificationFactory>  apiNotificationFactory();
}
