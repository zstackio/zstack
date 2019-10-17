package org.zstack.core.ansible;

import org.zstack.header.log.HasSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class RunAnsibleMsg extends NeedReplyMessage implements HasSensitiveInfo {
    private String targetIp;
    private String targetUuid;
    private String privateKeyFile;
    private String playBookPath;
    private String ansibleExecutable;
    @NoLogging
    private String remotePass;
    private Map<String, Object> arguments = new HashMap<String, Object>();

    public String getAnsibleExecutable() {
        return ansibleExecutable;
    }

    public void setAnsibleExecutable(String ansibleExecutable) {
        this.ansibleExecutable = ansibleExecutable;
    }

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

    public String getPlayBookPath() {
        return playBookPath;
    }

    public void setPlayBookPath(String playBookPath) {
        this.playBookPath = playBookPath;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public String getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(String targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getRemotePass() {
        return remotePass;
    }

    public void setRemotePass(String remotePass) {
        this.remotePass = remotePass;
    }
}
