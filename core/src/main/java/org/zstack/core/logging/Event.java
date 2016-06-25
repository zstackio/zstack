package org.zstack.core.logging;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by xing5 on 2016/6/3.
 */
public class Event extends Log {
    public static final String EVENT_PATH = "/logging/event";

    @Autowired
    private EventFacade evtf;

    public Event() {
        this(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
    }

    public Event(String accountUuid) {
        super(accountUuid);
        type = LogType.EVENT;
    }

    @NeedJsonSchema
    public static class EventContent extends Content {
        public String message;

        public EventContent() {
        }

        public EventContent(Content other) {
            super(other);
        }
    }

    @Override
    protected Event write() {
        EventContent c = new EventContent(getContent());
        c.message = toString();
        evtf.fire(EVENT_PATH, c);
        super.write();
        return this;
    }

    @ExceptionSafe
    public void log(String label, Object...args) {
        setText(label, args).write();
    }
}
