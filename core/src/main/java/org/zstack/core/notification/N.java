package org.zstack.core.notification;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.exception.CloudRuntimeException;
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

    public static N New(Class resourceClass, String resourceUuid) {
        return new N(resourceClass, resourceUuid);
    }

    private N() {
        builder = new NotificationBuilder();
        builder.name(NotificationConstant.SYSTEM_SENDER).sender(NotificationConstant.SYSTEM_SENDER);
    }

    private N(Class resourceClass, String resourceUuid) {
        super();

        NotificationAttributes at = (NotificationAttributes) resourceClass.getAnnotation(NotificationAttributes.class);
        if (at == null) {
            throw new CloudRuntimeException(String.format("class[%s] has no @NotificationAttributes defined", resourceClass));
        }

        builder.resource(resourceUuid, resourceClass.getSimpleName()).name(at.name());
    }

    private void send() {
        mgr.send(builder);
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
