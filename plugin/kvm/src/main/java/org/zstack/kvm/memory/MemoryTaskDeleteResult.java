package org.zstack.kvm.memory;

import java.util.ArrayList;
import java.util.List;

/** Trusted resource links copied from the locked task tree for API auditing. */
public class MemoryTaskDeleteResult {
    private String taskUuid;
    private boolean deleted;
    private String scope;
    private String resourceUuid;
    private List<String> hostUuids = new ArrayList<>();

    public String getTaskUuid() { return taskUuid; }
    public void setTaskUuid(String value) { taskUuid = value; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean value) { deleted = value; }
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }
    public List<String> getHostUuids() { return hostUuids; }
    public void setHostUuids(List<String> value) { hostUuids = value; }
}
