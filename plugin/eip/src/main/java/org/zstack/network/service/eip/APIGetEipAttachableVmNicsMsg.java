package org.zstack.network.service.eip;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.service.vip.VipVO;

/**
 */
@Action(category = EipConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/eips/{eipUuid}/vm-instances/candidate-nics",
        method = HttpMethod.GET,
        responseClass = APIGetEipAttachableVmNicsReply.class
)
public class APIGetEipAttachableVmNicsMsg extends APIGetMessage {
    @APIParam(required = false, resourceType = EipVO.class)
    private String eipUuid;
    @APIParam(required = false, resourceType = VipVO.class)
    private String vipUuid;
    @APIParam(required = false)
    private String vmUuid;
    @APIParam(required = false)
    private String vmName;

    public String getEipUuid() {
        return eipUuid;
    }

    public void setEipUuid(String eipUuid) {
        this.eipUuid = eipUuid;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public static APIGetEipAttachableVmNicsMsg __example__() {
        APIGetEipAttachableVmNicsMsg msg = new APIGetEipAttachableVmNicsMsg();
        msg.setEipUuid(uuid());

        return msg;
    }

}
