package org.zstack.header.core.external.plugin;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/external/plugins/{uuid}",
        responseClass = APIRefreshPluginDriversEvent.class,
        method = HttpMethod.DELETE
)
public class APIDeletePluginDriversMsg extends APIMessage {
    @APIParam(resourceType = PluginDriverVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeletePluginDriversMsg __example__() {
        APIDeletePluginDriversMsg msg = new APIDeletePluginDriversMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
