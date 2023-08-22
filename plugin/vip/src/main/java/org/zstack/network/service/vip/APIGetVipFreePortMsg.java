package org.zstack.network.service.vip;
import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.message.APIGetMessage;
import org.zstack.network.service.vip.*;

@RestRequest(
        path = "/vips/{vipUuid}/get-vip-free-port",
        method = HttpMethod.GET,
        responseClass = APIGetVipFreePortReply.class
)
public class APIGetVipFreePortMsg extends APIGetMessage {

    @APIParam(resourceType = VipVO.class)
    private String vipUuid;    

    @APIParam(validValues = {"TCP", "UDP"})
    private String protocolType;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }
 
    public static APIGetVipFreePortMsg __example__() {
        APIGetVipFreePortMsg msg = new APIGetVipFreePortMsg();
        msg.setVipUuid(uuid());
        msg.setProtocolType("UDP");
        msg.setStart(1);
        msg.setLimit(10);
        return msg;
    }
}
