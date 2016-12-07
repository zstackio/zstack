package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by luchukun on 8/4/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/ssh-keys",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmSshKeyEvent.class,
        parameterName = "null"
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
}
