package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 10/17/2015.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/iso",
        method = HttpMethod.DELETE,
        responseClass = APIDetachIsoFromVmInstanceEvent.class
)
public class APIDetachIsoFromVmInstanceMsg extends APIMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;

    // resourceType can not be set to ImageVO.class, because the image may have been deleted
    // required can not be set to true, Because of the need to be compatible with the old API
    @APIParam(required = false)
    private String isoUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getIsoUuid() {
        return isoUuid;
    }

    public void setIsoUuid(String isoUuid) {
        this.isoUuid = isoUuid;
    }

    public static APIDetachIsoFromVmInstanceMsg __example__() {
        APIDetachIsoFromVmInstanceMsg msg = new APIDetachIsoFromVmInstanceMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIDetachIsoFromVmInstanceMsg)msg).isoUuid, ImageVO.class);
    }
}
