package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/hosts/{uuid}/block-devices",
        method = HttpMethod.GET,
        responseClass = APIGetBlockDevicesEvent.class
)
public class APIGetBlockDevicesMsg extends APIMessage implements HostMessage {
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

    public static APIGetBlockDevicesMsg __example__() {
        APIGetBlockDevicesMsg msg = new APIGetBlockDevicesMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
