package org.zstack.kvm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.CarrierMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.concurrent.TimeUnit;

/**
 */
public class KVMHostAsyncHttpCallMsg extends NeedReplyMessage implements HostMessage, CarrierMessage {
    private String path;
    private String command;
    private String hostUuid;
    private boolean noStatusCheck;
    private long commandTimeout = -1;
    private String commandClassName;

    public String getCommandClassName() {
        return commandClassName;
    }

    @Override
    public long getTimeout() {
        return getCommandTimeout() + TimeUnit.SECONDS.toMillis(30);
    }

    public long getCommandTimeout() {
        assert commandTimeout != -1 : "commandTimeout is not set";
        assert commandTimeout != 0 : "commandTimeout cannot be 0";
        return commandTimeout;
    }

    public void setCommandTimeout(long commandTimeout) {
        this.commandTimeout = commandTimeout;
        timeout = getTimeout();
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
        commandClassName = command.getClass().getName();
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
