package org.zstack.header.network;

import org.zstack.header.message.NeedReplyMessage;

public class PrepareZnsSegmentMutationMsg extends NeedReplyMessage {
    private NetworkConfigMutation mutation;
    private String controllerUuid;
    private String segmentUuid;
    private String relationUuid;
    private long expectedConfigVersion;

    public NetworkConfigMutation getMutation() {
        return mutation;
    }

    public void setMutation(NetworkConfigMutation mutation) {
        this.mutation = mutation;
    }

    public String getControllerUuid() {
        return controllerUuid;
    }

    public void setControllerUuid(String controllerUuid) {
        this.controllerUuid = controllerUuid;
    }

    public String getSegmentUuid() {
        return segmentUuid;
    }

    public void setSegmentUuid(String segmentUuid) {
        this.segmentUuid = segmentUuid;
    }

    public String getRelationUuid() {
        return relationUuid;
    }

    public void setRelationUuid(String relationUuid) {
        this.relationUuid = relationUuid;
    }

    public long getExpectedConfigVersion() {
        return expectedConfigVersion;
    }

    public void setExpectedConfigVersion(long expectedConfigVersion) {
        this.expectedConfigVersion = expectedConfigVersion;
    }
}
