package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmClockTrackEvent.class
)
public class APISetVmClockTrackMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"guest", "host"})
    private String track;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public static APISetVmClockTrackMsg __example__() {
        APISetVmClockTrackMsg msg = new APISetVmClockTrackMsg();
        msg.uuid = uuid();
        msg.track = "guest";
        return msg;
    }
}
