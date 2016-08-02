package org.zstack.header.vm;

/**
 * Created by root on 7/29/16.
 */
import org.zstack.header.message.APIReply;

import java.util.List;


public class APIGetVmConsolePasswordReply extends APIReply {
    private String consolePassword;

    public String getConsolePassword() {
        return consolePassword;
    }

    public void setConsolePassword(String consolePassword) {
        this.consolePassword = consolePassword;
    }
}

