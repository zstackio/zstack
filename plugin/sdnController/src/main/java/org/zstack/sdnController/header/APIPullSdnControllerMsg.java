package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(path = "/sdn-controllers/{uuid}/resources/actions", method = HttpMethod.PUT,
        responseClass = APIPullSdnControllerEvent.class, isAction = true)
@Action(category = SdnControllerConstant.ACTION_CATEGORY)
public class APIPullSdnControllerMsg extends APIMessage implements SdnControllerMessage {
    @APIParam(resourceType = SdnControllerVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"Segment", "TenantRouter"})
    private String resourceType;
    @APIParam(required = false)
    private List<String> resourceUuids;

    @Override public String getSdnControllerUuid() { return uuid; }
    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String value) { resourceType = value; }
    public List<String> getResourceUuids() { return resourceUuids; }
    public void setResourceUuids(List<String> value) { resourceUuids = value; }
}
