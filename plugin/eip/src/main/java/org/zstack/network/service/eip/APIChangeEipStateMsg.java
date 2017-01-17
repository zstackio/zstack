package org.zstack.network.service.eip;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = EipConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/eips/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeEipStateEvent.class,
        isAction = true
)
public class APIChangeEipStateMsg extends APIMessage implements EipMessage {
    @APIParam(resourceType = EipVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

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
    public String getEipUuid() {
        return uuid;
    }
 
    public static APIChangeEipStateMsg __example__() {
        APIChangeEipStateMsg msg = new APIChangeEipStateMsg();


        return msg;
    }

}
