package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.rest.RestRequest;

@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/memory-policies/{resourceUuid}/preview", method = HttpMethod.POST, responseClass = APIPreviewMemoryPolicyReply.class, parameterName = "params")
public class APIPreviewMemoryPolicyMsg extends APISyncCallMessage {
    @APIParam(required = false, validValues = {"Global", "Cluster", "Host", "VM"})
    private String scope;
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }

    @APIParam(maxLength = 64)
    private String resourceUuid;
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }

    // Policy size is admitted by MemoryApiRequestBudget in the MN entrypoint.
    @APIParam
    private String policy;
    public String getPolicy() { return policy; }
    public void setPolicy(String value) { policy = value; }

    @APIParam(required = false)
    private java.util.List<String> targetHostUuids;
    public java.util.List<String> getTargetHostUuids() { return targetHostUuids; }
    public void setTargetHostUuids(java.util.List<String> value) { targetHostUuids = value; }

    @APIParam(required = false, validValues = {"apply", "clearOverride"})
    private String action = "apply";
    public String getAction() { return action == null ? "apply" : action; }
    public void setAction(String value) { action = value; }

    @APIParam(required = false)
    private java.util.List<String> clearOverrideFields;
    public java.util.List<String> getClearOverrideFields() { return clearOverrideFields; }
    public void setClearOverrideFields(java.util.List<String> value) { clearOverrideFields = value; }

    public static APIPreviewMemoryPolicyMsg __example__() {
        APIPreviewMemoryPolicyMsg msg = new APIPreviewMemoryPolicyMsg();
        msg.setResourceUuid("1234567890abcdef1234567890abcdef");
        msg.setPolicy("{\"ksm\":{\"enabled\":true}}");
        msg.setAction("apply");
        return msg;
    }
}
