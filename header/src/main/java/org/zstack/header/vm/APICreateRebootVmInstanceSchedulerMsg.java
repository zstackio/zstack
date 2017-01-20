package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.APICreateSchedulerMessage;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by root on 8/16/16.
 */

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmUuid}/schedulers/rebooting",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APICreateRebootVmInstanceSchedulerEvent.class
)
public class APICreateRebootVmInstanceSchedulerMsg extends APICreateSchedulerMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getVmUuid();
    }
 
    public static APICreateRebootVmInstanceSchedulerMsg __example__() {
        APICreateRebootVmInstanceSchedulerMsg msg = new APICreateRebootVmInstanceSchedulerMsg();
        msg.setSchedulerName("vm-scheduler");
        msg.setSchedulerDescription("for test restart vm scheduler");
        msg.setStartTime(0L);
        msg.setType("simple");
        msg.setInterval(5);
        msg.setRepeatCount(10);
        msg.setVmUuid(uuid());
        return msg;
    }

}
