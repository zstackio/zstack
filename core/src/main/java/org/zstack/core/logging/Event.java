package org.zstack.core.logging;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.identity.AccountConstant;

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

    @Override
    public void write() {
        evtf.fire(EVENT_PATH, getContent());
        super.write();
    }

    public Event log(String label, Object...args) {
        setText(label, args).write();
        return this;
    }
}
