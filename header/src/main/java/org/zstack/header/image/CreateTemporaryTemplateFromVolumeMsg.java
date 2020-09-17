package org.zstack.header.image;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2020/9/14.
 */
public class CreateTemporaryTemplateFromVolumeMsg extends NeedReplyMessage {
    private String volumeUuid;
    private SessionInventory session;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }
}
