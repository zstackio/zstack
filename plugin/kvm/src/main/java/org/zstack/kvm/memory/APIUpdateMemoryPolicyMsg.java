package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.StrictRestRequestJson;

import java.util.List;

@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/memory-policies/{resourceUuid}/actions", method = HttpMethod.PUT, responseClass = APIUpdateMemoryPolicyEvent.class, isAction = true)
@StrictRestRequestJson
public class APIUpdateMemoryPolicyMsg extends APIMessage implements APIMultiAuditor {
    @APIParam(required = false)
    private MemoryUncertainRecovery recovery;
    public MemoryUncertainRecovery getRecovery() { return recovery; }
    public void setRecovery(MemoryUncertainRecovery value) { recovery = value; }
    @APIParam(required = false)
    private MemoryBackendPreparation backendPreparation;
    public MemoryBackendPreparation getBackendPreparation() { return backendPreparation; }
    public void setBackendPreparation(MemoryBackendPreparation value) { backendPreparation = value; }
    @APIParam(required = false)
    private MemoryZramPoolPreparation poolPreparation;
    public MemoryZramPoolPreparation getPoolPreparation() { return poolPreparation; }
    public void setPoolPreparation(MemoryZramPoolPreparation value) { poolPreparation = value; }
    @APIParam(required = false)
    private java.util.Map<String, Long> expectedSourceRevisions;
    public java.util.Map<String, Long> getExpectedSourceRevisions() { return expectedSourceRevisions; }
    public void setExpectedSourceRevisions(java.util.Map<String, Long> value) { expectedSourceRevisions = value; }
    @APIParam(required = false)
    private java.util.List<String> clearOverrideFields;
    public java.util.List<String> getClearOverrideFields() { return clearOverrideFields; }
    public void setClearOverrideFields(java.util.List<String> value) { clearOverrideFields = value; }
    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
    private Long expectedGlobalRevision;
    public Long getExpectedGlobalRevision() { return expectedGlobalRevision; }
    public void setExpectedGlobalRevision(Long value) { expectedGlobalRevision = value; }
    @APIParam(required = false, maxLength = 128)
    private String expectedInstanceGeneration;
    public String getExpectedInstanceGeneration() { return expectedInstanceGeneration; }
    public void setExpectedInstanceGeneration(String value) { expectedInstanceGeneration = value; }

    @APIParam(required = false)
    private java.util.List<String> targetHostUuids;
    public java.util.List<String> getTargetHostUuids() { return targetHostUuids; }
    public void setTargetHostUuids(java.util.List<String> value) { targetHostUuids = value; }
    @APIParam(required = false, validValues = {"Global", "Cluster", "Host", "VM"})
    private String scope;
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }

    @APIParam(maxLength = 64)
    private String resourceUuid;
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }

    @APIParam(validValues = {"apply", "clearOverride", "pause", "drain", "resume", "reconcile", "recoverUncertain", "prepareWritebackBackend", "prepareZramPool", "stageTargetShard", "commitTargetShards", "cancelTargetShards"})
    private String action;
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }

    @APIParam(numberRange = {0, Long.MAX_VALUE})
    private long expectedRevision;
    public long getExpectedRevision() { return expectedRevision; }
    public void setExpectedRevision(long value) { expectedRevision = value; }

    @APIParam(required = false, maxLength = 32)
    private String expectedControlOperationUuid;
    public String getExpectedControlOperationUuid() { return expectedControlOperationUuid; }
    public void setExpectedControlOperationUuid(String value) { expectedControlOperationUuid = value; }

    // Policy size is admitted by MemoryApiRequestBudget in the MN entrypoint;
    // the framework's character limit would reject valid UTF-8 payloads first.
    @APIParam(required = false)
    private String policy = "{}";
    public String getPolicy() { return policy; }
    public void setPolicy(String value) { policy = value; }

    @APIParam(maxLength = 64)
    private String clientRequestUuid;
    public String getClientRequestUuid() { return clientRequestUuid; }
    public void setClientRequestUuid(String value) { clientRequestUuid = value; }

    @APIParam(required = false, maxLength = 128)
    private String targetSnapshotGeneration;
    public String getTargetSnapshotGeneration() { return targetSnapshotGeneration; }
    public void setTargetSnapshotGeneration(String value) { targetSnapshotGeneration = value; }

    @APIParam(required = false, numberRange = {0, Integer.MAX_VALUE})
    private Integer targetShardIndex;
    public Integer getTargetShardIndex() { return targetShardIndex; }
    public void setTargetShardIndex(Integer value) { targetShardIndex = value; }

    @APIParam(required = false, numberRange = {1, Integer.MAX_VALUE})
    private Integer targetShardCount;
    public Integer getTargetShardCount() { return targetShardCount; }
    public void setTargetShardCount(Integer value) { targetShardCount = value; }

    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
    private Long targetTotalCount;
    public Long getTargetTotalCount() { return targetTotalCount; }
    public void setTargetTotalCount(Long value) { targetTotalCount = value; }

    @APIParam(required = false, maxLength = 128)
    private String targetDigest;
    public String getTargetDigest() { return targetDigest; }
    public void setTargetDigest(String value) { targetDigest = value; }

    @APIParam(required = false)
    private java.util.List<String> targetVmUuids;
    public java.util.List<String> getTargetVmUuids() { return targetVmUuids; }
    public void setTargetVmUuids(java.util.List<String> value) { targetVmUuids = value; }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        return MemoryApiAuditHelper.update((APIUpdateMemoryPolicyMsg) msg, rsp);
    }

    public static APIUpdateMemoryPolicyMsg __example__() {
        APIUpdateMemoryPolicyMsg msg = new APIUpdateMemoryPolicyMsg();
        msg.setResourceUuid("1234567890abcdef1234567890abcdef");
        msg.setAction("apply");
        msg.setExpectedRevision(0L);
        java.util.Map<String, Long> sources = new java.util.LinkedHashMap<>();
        sources.put("Global:global", 0L);
        sources.put("Cluster:234567890abcdef1234567890abcdef1", 0L);
        sources.put("Host:" + msg.getResourceUuid(), 0L);
        msg.setExpectedSourceRevisions(sources);
        msg.setClientRequestUuid("abcdef1234567890abcdef1234567890");
        msg.setPolicy("{\"ksm\":{\"enabled\":true}}");
        return msg;
    }
}
