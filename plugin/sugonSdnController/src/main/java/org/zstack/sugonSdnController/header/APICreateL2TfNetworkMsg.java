package org.zstack.sugonSdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.network.l2.APICreateL2NetworkEvent;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

@OverriddenApiParams({
        @OverriddenApiParam(field = "physicalInterface", param = @APIParam(maxLength = 1024, required = false)),
        @OverriddenApiParam(field = "zoneUuid", param = @APIParam(maxLength = 1024, required = false, resourceType = ZoneVO.class))
})
@RestRequest(
        path = "/l2-networks/tf",
        method = HttpMethod.POST,
        responseClass = APICreateL2NetworkEvent.class,
        parameterName = "params"
)
public class APICreateL2TfNetworkMsg extends APICreateL2NetworkMsg {

    @APIParam(required = false, maxLength = 255)
    private String ipPrefix;

    @APIParam(required = false, numberRange = {1, 32})
    private Integer ipPrefixLength;

    public String getIpPrefix() {
        return ipPrefix;
    }

    public void setIpPrefix(String ipPrefix) {
        this.ipPrefix = ipPrefix;
    }

    public Integer getIpPrefixLength() {
        return ipPrefixLength;
    }

    public void setIpPrefixLength(Integer ipPrefixLength) {
        this.ipPrefixLength = ipPrefixLength;
    }

    @Override
    public String getType() {
        return SugonSdnControllerConstant.L2_TF_NETWORK_TYPE;
    }

    public static APICreateL2TfNetworkMsg __example__() {
        APICreateL2TfNetworkMsg msg = new APICreateL2TfNetworkMsg();
        msg.setName("Tf-L2-Network");
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());
        return msg;
    }
}
