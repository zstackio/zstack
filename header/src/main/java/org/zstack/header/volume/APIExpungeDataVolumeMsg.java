package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 11/16/2015.
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volumes/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIExpungeDataVolumeEvent.class
)
public class APIExpungeDataVolumeMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
 
    public static APIExpungeDataVolumeMsg __example__() {
        APIExpungeDataVolumeMsg msg = new APIExpungeDataVolumeMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
