package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.rest.RestRequest;

@Action(category = org.zstack.header.host.HostConstant.ACTION_CATEGORY, adminOnly = true, names = {"read"})
@RestRequest(path = "/memory-policies/{resourceUuid}", method = HttpMethod.GET, responseClass = APIGetMemoryPolicyReply.class)
public class APIGetMemoryPolicyMsg extends APISyncCallMessage {
    @APIParam(required = false, validValues = {"Global", "Cluster", "Host", "VM"})
    private String scope;
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }

    @APIParam(maxLength = 64)
    private String resourceUuid;
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }

    public static APIGetMemoryPolicyMsg __example__() {
        APIGetMemoryPolicyMsg msg = new APIGetMemoryPolicyMsg();
        msg.setResourceUuid("1234567890abcdef1234567890abcdef");
        return msg;
    }
}
