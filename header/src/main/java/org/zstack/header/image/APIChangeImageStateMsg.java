package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 5:31 PM
 * To change this template use File | Settings | File Templates.
 */
@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/images/{uuid}/actions",
        isAction = true,
        responseClass = APIChangeImageStateEvent.class,
        method = HttpMethod.PUT
)
public class APIChangeImageStateMsg extends APIMessage implements ImageMessage {
    @APIParam(resourceType = ImageVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    @Override
    public String getImageUuid() {
        return getUuid();
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
 
    public static APIChangeImageStateMsg __example__() {
        APIChangeImageStateMsg msg = new APIChangeImageStateMsg();

        msg.setUuid(uuid());
        msg.setStateEvent(ImageStateEvent.disable.toString());

        return msg;
    }
}
