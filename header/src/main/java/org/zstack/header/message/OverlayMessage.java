package org.zstack.header.message;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by xing5 on 2016/7/21.
 */
public class OverlayMessage extends NeedReplyMessage {
    protected Object message;
    protected String messageClassName;

    public void setMessage(NeedReplyMessage msg) {
        message = msg;
        messageClassName = msg.getClass().getName();
    }

    public NeedReplyMessage getMessage() {
        try {
            Class clazz = Class.forName(messageClassName);
            return (NeedReplyMessage) JSONObjectUtil.rehashObject(message, clazz);
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
