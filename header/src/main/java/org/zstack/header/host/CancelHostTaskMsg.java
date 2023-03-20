package org.zstack.header.host;

import org.zstack.header.message.CancelMessage;

/**
 * Created by MaJin on 2019/7/23.
 */
public class CancelHostTaskMsg extends CancelMessage implements HostMessage {
    private String hostUuid;
    private Integer sleepTime;
    private Integer retryInterval;

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Integer getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(Integer sleepTime) {
        this.sleepTime = sleepTime;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
