package org.zstack.image;

import org.zstack.core.notification.Notification;
import org.zstack.core.notification.NotificationBuilder;
import org.zstack.core.notification.NotificationConstant;
import org.zstack.core.notification.NotificationType;

@Notification(
        names = {"image"},
        resourceType = "ImageVO",
        sender = ""
)
public class ImageNotification {
    public static class Builder {
        String notificationName;
        String content;
        String sender = NotificationConstant.SYSTEM_SENDER;
        String resourceUuid;
        String resourceType = "ImageVO";
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
    
    public static Builder imageInfo_(String content, Object...args) {
        return new Builder(content, args).name("image").type(NotificationType.Info);
    }


    public static Builder imageWarn_(String content, Object...args) {
        return new Builder(content, args).name("image").type(NotificationType.Warning);
    }


    public static Builder imageError_(String content, Object...args) {
        return new Builder(content, args).name("image").type(NotificationType.Error);
    }


    public static Builder imageNotify_(String content, Object...args) {
        return new Builder(content, args).name("image");
    }

}
