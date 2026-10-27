package org.zstack.kvm.memory;

public class MemoryPolicyInventory {
    private MemoryVmExclusionInventory migrationExclusion;
    public MemoryVmExclusionInventory getMigrationExclusion() { return migrationExclusion; }
    public void setMigrationExclusion(MemoryVmExclusionInventory value) { migrationExclusion = value; }
    private java.util.Map<String, Long> sourceRevisions;
    private java.util.Map<String, String> fieldSources;
    /** Read-only semantic state for fields that are explicitly unmanaged. */
    private java.util.Map<String, String> fieldModes;
    public java.util.Map<String, Long> getSourceRevisions() { return sourceRevisions; }
    public void setSourceRevisions(java.util.Map<String, Long> value) { sourceRevisions = value; }
    public java.util.Map<String, String> getFieldSources() { return fieldSources; }
    public void setFieldSources(java.util.Map<String, String> value) { fieldSources = value; }
    public java.util.Map<String, String> getFieldModes() { return fieldModes; }
    public void setFieldModes(java.util.Map<String, String> value) { fieldModes = value; }
    private long sourceRevision;
    public long getSourceRevision() { return sourceRevision; }
    public void setSourceRevision(long value) { sourceRevision = value; }
    private String scope;
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }

    private String resourceUuid;
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }

    private long revision;
    public long getRevision() { return revision; }
    public void setRevision(long value) { revision = value; }

    private String policy;
    public String getPolicy() { return policy; }
    public void setPolicy(String value) { policy = value; }

    private String effectivePolicy;
    public String getEffectivePolicy() { return effectivePolicy; }
    public void setEffectivePolicy(String value) { effectivePolicy = value; }

}
