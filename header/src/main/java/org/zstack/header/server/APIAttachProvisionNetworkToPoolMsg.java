package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
    path = "/provision-networks/{networkUuid}/pools/{poolUuid}",
    method = HttpMethod.POST,
    responseClass = APIAttachProvisionNetworkToPoolEvent.class
)
public class APIAttachProvisionNetworkToPoolMsg extends APIMessage {
    @APIParam(resourceType = PhysicalServerProvisionNetworkVO.class)
    private String networkUuid;

    @APIParam(resourceType = ServerPoolVO.class)
    private String poolUuid;

    public String getNetworkUuid() { return networkUuid; }
    public void setNetworkUuid(String networkUuid) { this.networkUuid = networkUuid; }

    public String getPoolUuid() { return poolUuid; }
    public void setPoolUuid(String poolUuid) { this.poolUuid = poolUuid; }

    public static APIAttachProvisionNetworkToPoolMsg __example__() {
        APIAttachProvisionNetworkToPoolMsg msg = new APIAttachProvisionNetworkToPoolMsg();
        msg.setNetworkUuid(uuid());
        msg.setPoolUuid(uuid());
        return msg;
    }
}
