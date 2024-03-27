package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:07 2020/2/27
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateVmNicDriverEvent.class
)
public class APIUpdateVmNicDriverMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;

    @APIParam(resourceType = VmNicVO.class, nonempty = true, checkAccount = true)
    private String vmNicUuid;

    @APIParam
    private String driverType;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public static APIUpdateVmNicDriverMsg __example__() {
        APIUpdateVmNicDriverMsg msg = new APIUpdateVmNicDriverMsg();
        msg.vmInstanceUuid = uuid();
        msg.vmNicUuid = uuid();
        msg.driverType = VmNicConstant.NIC_DRIVER_TYPE_E1000;
        return msg;
    }
}
