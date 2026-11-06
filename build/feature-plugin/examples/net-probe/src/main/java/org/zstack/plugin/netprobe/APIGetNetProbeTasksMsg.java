package org.zstack.plugin.netprobe;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/net-probe/tasks",
        method = HttpMethod.GET,
        responseClass = APIGetNetProbeTasksReply.class
)
public class APIGetNetProbeTasksMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
