package org.zstack.core.notification;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by xing5 on 2017/5/8.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class N {
    private static CLogger logger = Utils.getLogger(N.class);

    private NotificationBuilder builder;

    @Autowired
    private NotificationManager mgr;

    public static N New() {
        return new N();
    }

    public static N New(String resourceType, String resourceUuid) {
        return new N(resourceType, resourceUuid);
    }

    public static N New(Class resourceClass, String resourceUuid) {
        DebugUtils.Assert(resourceClass != null, "resourceClass cannot be null");
        return new N(resourceClass, resourceUuid);
    }

    private N() {
        builder = new NotificationBuilder();
        builder.name(NotificationConstant.SYSTEM_SENDER).sender(NotificationConstant.SYSTEM_SENDER);
    }

    private N(String resourceType, String resourceUuid) {
        this();
        builder.resource(resourceUuid, resourceType);
    }

    private N(Class resourceClass, String resourceUuid) {
        this();
        builder.resource(resourceUuid, resourceClass.getSimpleName());
    }

    private void send() {
        mgr.send(builder);
    }

    public N sender(String sender) {
        builder.name(sender).sender(sender);
        return this;
    }

    public void warn_(String fmt, Object...args) {
        builder.content(fmt).arguments(args).type(NotificationType.Warning);
        send();
        logger.warn(String.format(fmt, args));
    }

    public void info_(String fmt, Object...args) {
        builder.content(fmt).arguments(args).type(NotificationType.Info);
        send();
        logger.info(String.format(fmt, args));
    }

    public void error_(String fmt, Object...args) {
        builder.content(fmt).arguments(args).type(NotificationType.Error);
        send();
        logger.error(String.format(fmt, args));
    }

    public N opaque(Object o) {
        builder.opaque(o);
        return this;
    }
}
