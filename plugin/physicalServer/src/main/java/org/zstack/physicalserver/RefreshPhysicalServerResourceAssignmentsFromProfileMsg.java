package org.zstack.physicalserver;

import org.zstack.header.message.NeedReplyMessage;

public class RefreshPhysicalServerResourceAssignmentsFromProfileMsg extends NeedReplyMessage
        implements PhysicalServerMessage {
    private String serverUuid;
    private String profileSha256;

    @Override
    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public String getProfileSha256() {
        return profileSha256;
    }

    public void setProfileSha256(String profileSha256) {
        this.profileSha256 = profileSha256;
    }
}
