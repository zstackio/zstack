package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APISetVmMachineTypeEvent.class
)
public class APISetVmMachineTypeMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"q35"})
    private String machineType;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public static APISetVmMachineTypeMsg __example__() {
        APISetVmMachineTypeMsg msg = new APISetVmMachineTypeMsg();
        msg.uuid = uuid();
        msg.machineType = VmMachineType.q35.toString();
        return msg;
    }
}
