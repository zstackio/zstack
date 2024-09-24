package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/hosts/{uuid}/get-sensors",
        method = HttpMethod.GET,
        responseClass = APIGetHostSensorsReply.class
)
public class APIGetHostSensorsMsg extends APISyncCallMessage implements HostMessage {
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }

    public static APIGetHostSensorsMsg __example__() {
        APIGetHostSensorsMsg msg = new APIGetHostSensorsMsg();
        msg.setUuid(uuid());
        return msg;
    }
}