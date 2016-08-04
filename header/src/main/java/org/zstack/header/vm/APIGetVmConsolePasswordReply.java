package org.zstack.header.vm;

import org.zstack.header.message.APIReply;


/**
 * Created by root on 7/29/16.
 */



public class APIGetVmConsolePasswordReply extends APIReply {
    private String consolePassword;

    public String getConsolePassword() {
        return consolePassword;
    }

    public void setConsolePassword(String consolePassword) {
        this.consolePassword = consolePassword;
    }
}

