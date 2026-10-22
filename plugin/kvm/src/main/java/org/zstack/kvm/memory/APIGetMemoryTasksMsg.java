package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostConstant;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@Action(category = HostConstant.ACTION_CATEGORY, adminOnly = true, names = {"read"})
@RestRequest(path = "/memory-optimization/tasks", method = HttpMethod.GET, responseClass = APIQueryMemoryTaskReply.class)
public class APIGetMemoryTasksMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private String uuid;
    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }

    @APIParam(required = false)
    private String hostUuid;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }

    @APIParam(required = false)
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    @APIParam(required = false)
    private int start = 0;
    public int getStart() { return start; }
    public void setStart(int value) { start = value; }

    @APIParam(required = false)
    private Integer limit;
    public int getLimit() { return MemoryOptimizationGlobalConfig.pageSize(limit); }
    public void setLimit(int value) { limit = value; }
    @APIParam(required = false, maxLength = 128)
    private String snapshotId;
    public String getSnapshotId() { return snapshotId; }
    public void setSnapshotId(String value) { snapshotId = value; }

    public static APIGetMemoryTasksMsg __example__() {
        APIGetMemoryTasksMsg msg = new APIGetMemoryTasksMsg();
        msg.setStart(0);
        msg.setLimit(100);
        return msg;
    }
}
