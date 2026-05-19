package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

@Action(category = VolumeConstant.ACTION_CATEGORY)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
@RestRequest(
        path = "/volumes/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIReInitDataVolumeEvent.class
)
public class APIReInitDataVolumeMsg extends APIMessage implements VolumeMessage, APIAuditor {
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIReInitDataVolumeMsg) msg).getUuid(), VolumeVO.class);
    }

    public static APIReInitDataVolumeMsg __example__() {
        APIReInitDataVolumeMsg msg = new APIReInitDataVolumeMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
