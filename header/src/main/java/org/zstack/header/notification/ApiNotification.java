package org.zstack.header.notification;

import org.zstack.header.message.APIEvent;
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
        Boolean success;

        public Inner(String content, Object[] arguments) {
            this.content = content;
            this.arguments = arguments;
        }

        public Inner resource(String uuid, String type) {
            resourceUuid = uuid;
            resourceType = type;
            return this;
        }

        public Inner successOrNot(APIEvent evt) {
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

}
