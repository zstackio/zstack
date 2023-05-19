package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author : jingwang
 * @create 2023/4/25 2:26 PM
 */
public class GetHostWebSshUrlMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }
}
