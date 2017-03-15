package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.APICreateSchedulerMessage;
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by root on 7/30/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmUuid}/schedulers/stopping",
        responseClass = APICreateStopVmInstanceSchedulerEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
public class APICreateStopVmInstanceSchedulerMsg extends APICreateSchedulerMessage implements VmInstanceMessage {
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
 
    public static APICreateStopVmInstanceSchedulerMsg __example__() {
        APICreateStopVmInstanceSchedulerMsg msg = new APICreateStopVmInstanceSchedulerMsg();
        msg.setSchedulerName("vm-scheduler");
        msg.setSchedulerDescription("for test stop vm scheduler");
        msg.setStartTime(0L);
        msg.setType("simple");
        msg.setInterval(5);
        msg.setRepeatCount(10);
        msg.setVmUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Created").resource(((APICreateStopVmInstanceSchedulerEvent) evt).getInventory().getUuid(), SchedulerVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
