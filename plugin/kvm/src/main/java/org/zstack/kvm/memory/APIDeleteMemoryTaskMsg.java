package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/memory-optimization/tasks/{uuid}", method = HttpMethod.DELETE,
        responseClass = APIDeleteMemoryTaskEvent.class)
public class APIDeleteMemoryTaskMsg extends APIMessage implements APIMultiAuditor {
    @APIParam(maxLength = 32)
    private String uuid;

    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, org.zstack.header.message.APIEvent rsp) {
        return MemoryApiAuditHelper.delete((APIDeleteMemoryTaskMsg) msg, rsp);
    }

    public static APIDeleteMemoryTaskMsg __example__() {
        APIDeleteMemoryTaskMsg msg = new APIDeleteMemoryTaskMsg();
        msg.setUuid("1234567890abcdef1234567890abcdef");
        return msg;
    }
}
