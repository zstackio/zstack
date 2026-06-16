package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.volume.VolumeMessage;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeVO;

@RestRequest(
        path = "/volumes/{volumeUuid}/actions",
        responseClass = APIChangeVolumeProtocolEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APIChangeVolumeProtocolMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class, operationTarget = true)
    private String volumeUuid;

    @APIParam
    private String protocol;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public static APIChangeVolumeProtocolMsg __example__() {
        APIChangeVolumeProtocolMsg msg = new APIChangeVolumeProtocolMsg();
        msg.setVolumeUuid(uuid());
        msg.setProtocol(VolumeProtocol.Vhost.toString());
        return msg;
    }
}
