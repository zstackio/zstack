package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.SchedulerJobSchedulerTriggerRefVO;
import org.zstack.header.core.scheduler.SchedulerJobVO;
import org.zstack.header.core.scheduler.SchedulerStateEvent;
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by Mei Lei on 8/31/16.
 */
@Action(category = SchedulerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/schedulers/{uuid}",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeSchedulerStateEvent.class
)
public class APIChangeSchedulerStateMsg  extends APIMessage implements SchedulerMessage  {
    @APIParam(resourceType = SchedulerJobVO.class)
    private String uuid;
    @APIParam(validValues={"enable", "disable"})
    private String stateEvent;

    public APIChangeSchedulerStateMsg() {
    }

    public APIChangeSchedulerStateMsg(String uuid, String stateEvent) {
        this.uuid = uuid;
        this.stateEvent = stateEvent;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getSchedulerUuid() {
        return uuid;
    }
 
    public static APIChangeSchedulerStateMsg __example__() {
        APIChangeSchedulerStateMsg msg = new APIChangeSchedulerStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(SchedulerStateEvent.disable.toString());
        return msg;
    }

//    public ApiNotification __notification__() {
//        APIMessage that = this;
//
//        return new ApiNotification() {
//            @Override
//            public void after(APIEvent evt) {
//                ntfy("Changing the state to", ((APIChangeSchedulerStateEvent)evt).getInventory().getState())
//                        .resource(uuid, SchedulerVO.class.getSimpleName())
//                        .messageAndEvent(that, evt).done();
//            }
//        };
//    }
}
