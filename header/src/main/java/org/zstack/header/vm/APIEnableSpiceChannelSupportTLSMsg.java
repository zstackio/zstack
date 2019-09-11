package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIEnableSpiceChannelSupportTLSEvent.class
)
public class APIEnableSpiceChannelSupportTLSMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam
    private Boolean open;
    @APIParam(required = false)
    private List<String> channels;

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

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }

    public static APIEnableSpiceChannelSupportTLSMsg __example__() {
        APIEnableSpiceChannelSupportTLSMsg msg = new APIEnableSpiceChannelSupportTLSMsg();
        msg.uuid = uuid();
        msg.setOpen(true);
        msg.setChannels(Arrays.asList(SpiceChannelEnum.main.toString(), SpiceChannelEnum.display.toString()));
        return msg;
    }
}
