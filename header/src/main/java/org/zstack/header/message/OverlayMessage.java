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
            NeedReplyMessage nmsg = (NeedReplyMessage) JSONObjectUtil.rehashObject(message, clazz);
            if(nmsg instanceof OverlayMessage){
                OverlayMessage omsg = (OverlayMessage)nmsg;
                omsg.setMessage(omsg.getMessage());
                return omsg;
            }else {
                return nmsg;
            }
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
