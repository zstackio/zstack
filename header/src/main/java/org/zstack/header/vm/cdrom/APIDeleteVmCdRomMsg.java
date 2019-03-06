package org.zstack.header.vm.cdrom;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Create by lining at 2018/12/27
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/cdroms/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmCdRomEvent.class
)
public class APIDeleteVmCdRomMsg extends APIDeleteMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = VmCdRomVO.class, checkAccount = true, operationTarget = true, successIfResourceNotExisting = true)
    private String uuid;

    @APINoSee
    private String vmInstanceUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public static APIDeleteVmCdRomMsg __example__() {
        APIDeleteVmCdRomMsg msg = new APIDeleteVmCdRomMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIDeleteVmCdRomMsg) msg).getVmInstanceUuid() : "", VmInstanceVO.class);
    }
}
