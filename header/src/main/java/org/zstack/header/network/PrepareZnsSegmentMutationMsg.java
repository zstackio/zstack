package org.zstack.header.network;

import org.zstack.header.message.NeedReplyMessage;

public class PrepareZnsSegmentMutationMsg extends NeedReplyMessage {
    private NetworkConfigMutation mutation;
    private String controllerUuid;

    public NetworkConfigMutation getMutation() { return mutation; }
    public void setMutation(NetworkConfigMutation value) { mutation = value; }
    public String getControllerUuid() { return controllerUuid; }
    public void setControllerUuid(String value) { controllerUuid = value; }
}
