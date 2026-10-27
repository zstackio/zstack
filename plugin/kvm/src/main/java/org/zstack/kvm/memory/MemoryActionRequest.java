package org.zstack.kvm.memory;

import org.zstack.header.identity.SessionInventory;

/** An operation request on the per-Host queue, built by the memory LongJobs and internal recovery. */
public class MemoryActionRequest {
    private SessionInventory session;
    public SessionInventory getSession() { return session; }
    public void setSession(SessionInventory value) { session = value; }

    private MemoryUncertainRecovery recovery;
    public MemoryUncertainRecovery getRecovery() { return recovery; }
    public void setRecovery(MemoryUncertainRecovery value) { recovery = value; }
    private MemoryBackendPreparation backendPreparation;
    public MemoryBackendPreparation getBackendPreparation() { return backendPreparation; }
    public void setBackendPreparation(MemoryBackendPreparation value) { backendPreparation = value; }

    private String scope;
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }

    private String resourceUuid;
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }

    private String action;
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }

    private String expectedControlOperationUuid;
    public String getExpectedControlOperationUuid() { return expectedControlOperationUuid; }
    public void setExpectedControlOperationUuid(String value) { expectedControlOperationUuid = value; }

    private String policy = "{}";
    public String getPolicy() { return policy; }
    public void setPolicy(String value) { policy = value; }
}
