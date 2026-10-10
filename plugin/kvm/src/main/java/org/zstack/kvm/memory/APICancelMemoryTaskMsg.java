package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/memory-optimization/tasks/{uuid}/actions", optionalPaths = {"/memory-tasks/{uuid}/actions"},
        method = HttpMethod.PUT, responseClass = APICancelMemoryTaskEvent.class, isAction = true)
public class APICancelMemoryTaskMsg extends APIMessage implements APIMultiAuditor {
    @APIParam(maxLength = 32)
    private String uuid;
    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        return MemoryApiAuditHelper.cancel((APICancelMemoryTaskMsg) msg, rsp);
    }

    public static APICancelMemoryTaskMsg __example__() {
        APICancelMemoryTaskMsg msg = new APICancelMemoryTaskMsg();
        msg.setUuid("1234567890abcdef1234567890abcdef");
        return msg;
    }
}
