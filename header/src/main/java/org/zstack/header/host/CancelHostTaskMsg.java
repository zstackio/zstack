package org.zstack.header.host;

import org.zstack.header.message.CancelMessage;

/**
 * Created by MaJin on 2019/7/23.
 */
public class CancelHostTaskMsg extends CancelMessage implements HostMessage {
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
