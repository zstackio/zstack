package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by MaJin on 2019/7/26.
 */
public class GetHostTaskMsg extends NeedReplyMessage {
    private List<String> hostUuids;

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }
}
