package org.zstack.core.ansible;

import org.zstack.header.message.NeedReplyMessage;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class RunAnsibleMsg extends NeedReplyMessage {
    private String targetIp;
    private String privateKeyFile;
    private String playBookName;
    private Map<String, Object> arguments = new HashMap<String, Object>();

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    public String getPlayBookName() {
        return playBookName;
    }

    public void setPlayBookName(String playBookName) {
        this.playBookName = playBookName;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
