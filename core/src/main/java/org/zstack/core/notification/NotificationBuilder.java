package org.zstack.core.notification;

import org.zstack.core.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by xing5 on 2017/3/15.
 */
public class NotificationBuilder {
    String notificationName;
    String content;
    String sender = NotificationConstant.SYSTEM_SENDER;
    String resourceUuid;
    String resourceType;
    Object opaque;
    List arguments = new ArrayList();
    NotificationType type = NotificationType.Info;

    public NotificationBuilder name(String name) {
        notificationName = name;
        return this;
    }

    public NotificationBuilder content(String content) {
        this.content = content;
        return this;
    }


    public NotificationBuilder arguments(Object...args) {
        if (args != null) {
            Collections.addAll(arguments, args);
        }

        return this;
    }

    public NotificationBuilder sender(String sender) {
        this.sender = sender;
        return this;
    }

    public NotificationBuilder opaque(Object opaque) {
        this.opaque = opaque;
        return this;
    }

    public NotificationBuilder resource(String uuid, String type) {
        resourceUuid = uuid;
        resourceType = type;
        return this;
    }

    public NotificationBuilder type(NotificationType type) {
        this.type = type;
        return this;
    }

    public void send() {
        NotificationManager mgr = Platform.getComponentLoader().getComponent(NotificationManager.class);
        mgr.send(this);
    }
}
