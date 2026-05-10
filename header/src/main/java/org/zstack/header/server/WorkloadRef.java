package org.zstack.header.server;

/**
 * Reference to an active workload on a role (VM / BareMetal instance / Pod).
 * Used by {@link RoleWorkloadStatus#getActiveWorkloads()} to let callers render
 * detach/poweroff/maintenance rejection details to operators.
 */
public class WorkloadRef {
    private String uuid;
    private String name;
    private String type;
    private String state;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
