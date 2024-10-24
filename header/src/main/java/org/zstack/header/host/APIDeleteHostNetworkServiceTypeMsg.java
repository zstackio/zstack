package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by boce.wang on 10/25/2024.
 */
@Action(category = HostConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/hosts/service-types/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteHostNetworkServiceTypeEvent.class
)
public class APIDeleteHostNetworkServiceTypeMsg extends APIDeleteMessage {
    @APIParam(resourceType = HostNetworkLabelVO.class, successIfResourceNotExisting = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteHostNetworkServiceTypeMsg __example__() {
        APIDeleteHostNetworkServiceTypeMsg msg = new APIDeleteHostNetworkServiceTypeMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
