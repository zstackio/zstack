package org.zstack.storage.primary;

import org.zstack.core.notification.Notification;
import org.zstack.core.notification.NotificationBuilder;
import org.zstack.core.notification.NotificationConstant;
import org.zstack.core.notification.NotificationType;

@Notification(
        names = {"ps"},
        resourceType = "PrimaryStorageVO",
        sender = ""
)
public class PrimaryStorageNotification {
    public static class Builder {
        String notificationName;
        String content;
        String sender = NotificationConstant.SYSTEM_SENDER;
        String resourceUuid;
        String resourceType = "PrimaryStorageVO";
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
    
    public static Builder psInfo_(String content, Object...args) {
        return new Builder(content, args).name("ps").type(NotificationType.Info);
    }


    public static Builder psWarn_(String content, Object...args) {
        return new Builder(content, args).name("ps").type(NotificationType.Warning);
    }


    public static Builder psError_(String content, Object...args) {
        return new Builder(content, args).name("ps").type(NotificationType.Error);
    }


    public static Builder psNotify_(String content, Object...args) {
        return new Builder(content, args).name("ps");
    }

}
