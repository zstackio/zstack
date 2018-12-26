package org.zstack.header.vm.cdrom;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Create by lining at 2018/12/29
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/cdroms/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APISetVmInstanceDefaultCdRomEvent.class
)
public class APISetVmInstanceDefaultCdRomMsg extends APIMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = VmCdRomVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APISetVmInstanceDefaultCdRomMsg __example__() {
        APISetVmInstanceDefaultCdRomMsg msg = new APISetVmInstanceDefaultCdRomMsg();
        msg.uuid = uuid();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APISetVmInstanceDefaultCdRomEvent)rsp).getInventory().getUuid() : "", VmCdRomVO.class);
    }
}
