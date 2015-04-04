package org.zstack.header.identity;

import org.zstack.header.message.APIReply;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class APIValidateSessionReply extends APIReply {
    private boolean validSession;

    public boolean isValidSession() {
        return validSession;
    }

    public void setValidSession(boolean validSession) {
        this.validSession = validSession;
    }
}
