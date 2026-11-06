package org.zstack.plugin.netprobe;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/net-probe/tasks/{taskUuid}/results",
        method = HttpMethod.GET,
        responseClass = APIGetNetProbeResultsReply.class
)
public class APIGetNetProbeResultsMsg extends APISyncCallMessage {
    @APIParam
    private String taskUuid;

    @APIParam(required = false, numberRange = {1, 100})
    private Integer limit;

    public String getTaskUuid() {
        return taskUuid;
    }

    public void setTaskUuid(String taskUuid) {
        this.taskUuid = taskUuid;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
