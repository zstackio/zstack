package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by luchukun on 8/4/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/ssh-keys",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmSshKeyEvent.class
)
public class APIDeleteVmSshKeyMsg extends APIMessage implements VmInstanceMessage {
    private String uuid;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
 
    public static APIDeleteVmSshKeyMsg __example__() {
        APIDeleteVmSshKeyMsg msg = new APIDeleteVmSshKeyMsg();
        msg.uuid = uuid();
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("ssh key deleted").resource(uuid, VmInstanceVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
