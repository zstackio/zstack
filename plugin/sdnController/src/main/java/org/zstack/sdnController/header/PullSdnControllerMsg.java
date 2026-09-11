package org.zstack.sdnController.header;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;

import java.util.List;

public class PullSdnControllerMsg extends NeedReplyMessage implements SdnControllerMessage {
    private String sdnControllerUuid;
    private String resourceType;
    private List<String> resourceUuids;

    @Override public String getSdnControllerUuid() { return sdnControllerUuid; }
    public void setSdnControllerUuid(String value) { sdnControllerUuid = value; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String value) { resourceType = value; }
    public List<String> getResourceUuids() { return resourceUuids; }
    public void setResourceUuids(List<String> value) { resourceUuids = value; }
}
