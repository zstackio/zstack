package org.zstack.compute.vm;

import org.zstack.core.notification.Notification;
import org.zstack.core.notification.NotificationBuilder;
import org.zstack.core.notification.NotificationConstant;
import org.zstack.core.notification.NotificationType;

@Notification(
        names = {"vm"},
        resourceType = "VmInstanceVO",
        sender = ""
)
public class VmNotification {
    public static class Builder {
        String notificationName;
        String content;
        String sender = NotificationConstant.SYSTEM_SENDER;
        String resourceUuid;
        String resourceType = "VmInstanceVO";
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
    
    public static Builder vmInfo_(String content, Object...args) {
        return new Builder(content, args).name("vm").type(NotificationType.Info);
    }


    public static Builder vmWarn_(String content, Object...args) {
        return new Builder(content, args).name("vm").type(NotificationType.Warning);
    }


    public static Builder vmError_(String content, Object...args) {
        return new Builder(content, args).name("vm").type(NotificationType.Error);
    }


    public static Builder vmNotify_(String content, Object...args) {
        return new Builder(content, args).name("vm");
    }

}
