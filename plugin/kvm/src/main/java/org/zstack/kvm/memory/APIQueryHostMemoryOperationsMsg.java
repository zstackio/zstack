package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.host.HostVO;

@Action(category = org.zstack.header.host.HostConstant.ACTION_CATEGORY, adminOnly = true, names = {"read"})
@RestRequest(path = "/hosts/{hostUuid}/memory-operations", method = HttpMethod.GET,
        responseClass = APIQueryHostMemoryOperationsReply.class, strictQueryParameters = true)
@Deprecated
public class APIQueryHostMemoryOperationsMsg extends APISyncCallMessage {
    @APIParam(resourceType = HostVO.class) private String hostUuid;
    @APIParam(required = false) private String operationId;
    @APIParam(required = false) private String vmUuid;
    @APIParam(required = false) private String status;
    @APIParam(required = false, numberRange = {0, Integer.MAX_VALUE}) private int start;
    @APIParam(required = false) private Integer limit;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public String getOperationId() { return operationId; }
    public void setOperationId(String value) { operationId = value; }
    public String getVmUuid() { return vmUuid; }
    public void setVmUuid(String value) { vmUuid = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public int getStart() { return start; }
    public void setStart(int value) { start = value; }
    public int getLimit() { return MemoryOptimizationGlobalConfig.pageSize(limit); }
    public void setLimit(int value) { limit = value; }
    public static APIQueryHostMemoryOperationsMsg __example__() {
        APIQueryHostMemoryOperationsMsg msg = new APIQueryHostMemoryOperationsMsg();
        msg.setHostUuid("1234567890abcdef1234567890abcdef");
        msg.setStart(0);
        msg.setLimit(100);
        return msg;
    }
}
