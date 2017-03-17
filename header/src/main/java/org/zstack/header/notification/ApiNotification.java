package org.zstack.header.notification;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2017/3/16.
 */
public abstract class ApiNotification {
    private List<Inner> inners = new ArrayList<>();

    public class Inner {
        String content;
        Object[] arguments;
        String resourceUuid;
        String resourceType;
        APIMessage message;
        APIEvent event;
        Boolean success;

        public APIMessage getMessage() {
            return message;
        }

        public APIEvent getEvent() {
            return event;
        }

        public String getContent() {
            return content;
        }

        public Object[] getArguments() {
            return arguments;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public String getResourceType() {
            return resourceType;
        }

        public Boolean getSuccess() {
            return success;
        }

        public Inner(String content, Object[] arguments) {
            this.content = content;
            this.arguments = arguments;
        }

        public Inner resource(String uuid, String type) {
            resourceUuid = uuid;
            resourceType = type;
            return this;
        }

        public Inner messageAndEvent(APIMessage msg, APIEvent evt) {
            message = msg;
            event = evt;
            success = evt.isSuccess();
            return this;
        }

        public void done() {
            DebugUtils.Assert(success != null, "you must call successOrNot() before done()");
            inners.add(this);
        }
    }

    protected Inner ntfy(String content, Object...args) {
        return new Inner(content, args);
    }

    public abstract void makeNotifications();

    public List<Inner> getInners() {
        return inners;
    }

    public void setInners(List<Inner> inners) {
        this.inners = inners;
    }
}
