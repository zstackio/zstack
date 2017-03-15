package org.zstack.compute.host;

import org.zstack.core.notification.Notification;
import org.zstack.core.notification.NotificationBuilder;
import org.zstack.core.notification.NotificationConstant;
import org.zstack.core.notification.NotificationType;

@Notification(
        names = {"host"},
        resourceType = "HostVO",
        sender = ""
)
public class HostNotification {
    public static class Builder {
        String notificationName;
        String content;
        String sender = NotificationConstant.SYSTEM_SENDER;
        String resourceUuid;
        String resourceType = "HostVO";
        Object[] arguments;
        NotificationType type = NotificationType.Info;


        public Builder(String content, Object...args) {
            this.content = content;
            this.arguments = args;
        }

        private Builder name(String name) {
            notificationName = name;
            return this;
        }

        public Builder uuid(String uuid) {
            resourceUuid = uuid;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public void send() {
            new NotificationBuilder().name(notificationName)
                    .resource(resourceUuid, resourceType)
                    .sender(sender)
                    .type(type)
                    .content(content)
                    .arguments(arguments)
                    .send();
        }
    }
    
    public static Builder hostInfo_(String content, Object...args) {
        return new Builder(content, args).name("host").type(NotificationType.Info);
    }


    public static Builder hostWarn_(String content, Object...args) {
        return new Builder(content, args).name("host").type(NotificationType.Warning);
    }


    public static Builder hostError_(String content, Object...args) {
        return new Builder(content, args).name("host").type(NotificationType.Error);
    }


    public static Builder hostNotify_(String content, Object...args) {
        return new Builder(content, args).name("host");
    }

}
