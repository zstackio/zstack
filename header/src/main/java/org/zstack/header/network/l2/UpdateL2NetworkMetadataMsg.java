package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;

public class UpdateL2NetworkMetadataMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String l2NetworkUuid;
    private String name;
    private String description;
    private NetworkCreateContext context;

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NetworkCreateContext getContext() {
        return context;
    }

    public void setContext(NetworkCreateContext context) {
        this.context = context;
    }
}
