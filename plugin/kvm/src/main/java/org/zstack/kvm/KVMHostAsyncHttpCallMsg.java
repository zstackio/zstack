package org.zstack.kvm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 */
public class KVMHostAsyncHttpCallMsg extends NeedReplyMessage implements HostMessage {
    private String path;
    private String command;
    private String hostUuid;
    private boolean noStatusCheck;
    private int commandTimeout = 300;

    public int getCommandTimeout() {
        return commandTimeout;
    }

    public void setCommandTimeout(int commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public boolean isNoStatusCheck() {
        return noStatusCheck;
    }

    public void setNoStatusCheck(boolean noStatusCheck) {
        this.noStatusCheck = noStatusCheck;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(Object command) {
        this.command = JSONObjectUtil.toJsonString(command);
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
