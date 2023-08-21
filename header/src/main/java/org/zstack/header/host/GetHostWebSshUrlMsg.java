package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author : jingwang
 * @create 2023/4/25 2:26 PM
 */
public class GetHostWebSshUrlMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;

    private Boolean https;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Boolean getHttps() {
        return https;
    }

    public void setHttps(Boolean https) {
        this.https = https;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }
}
