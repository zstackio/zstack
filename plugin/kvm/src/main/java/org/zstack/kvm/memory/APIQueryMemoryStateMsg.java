package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.rest.RestRequest;

@Action(category = org.zstack.header.host.HostConstant.ACTION_CATEGORY, adminOnly = true, names = {"read"})
@RestRequest(path = "/memory-states", method = HttpMethod.GET, responseClass = APIQueryMemoryStateReply.class,
        strictQueryParameters = true)
@Deprecated
public class APIQueryMemoryStateMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = org.zstack.header.host.HostVO.class)
    private java.util.List<String> hostUuids;
    public java.util.List<String> getHostUuids() { return hostUuids; }
    public void setHostUuids(java.util.List<String> value) { hostUuids = value; }

    @APIParam(required = false, numberRange = {0, Integer.MAX_VALUE})
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

    public static APIQueryMemoryStateMsg __example__() {
        APIQueryMemoryStateMsg msg = new APIQueryMemoryStateMsg();
        msg.setHostUuids(java.util.Collections.singletonList("1234567890abcdef1234567890abcdef"));
        msg.setStart(0);
        msg.setLimit(100);
        return msg;
    }
}
