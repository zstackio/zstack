package org.zstack.plugin.netprobe;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/net-probe/tasks/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteNetProbeTaskEvent.class
)
public class APIDeleteNetProbeTaskMsg extends APIMessage {
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
