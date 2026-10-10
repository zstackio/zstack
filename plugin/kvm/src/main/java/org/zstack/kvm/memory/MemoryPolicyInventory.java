package org.zstack.kvm.memory;

/** Public API inventory. Nullable samples represent unknown, never a fabricated zero. */
public class MemoryPolicyInventory {
    public static MemoryPolicyInventory __example__() {
        MemoryPolicyInventory inventory = new MemoryPolicyInventory();
        inventory.setScope("Host");
        inventory.setResourceUuid("1234567890abcdef1234567890abcdef");
        inventory.setRevision(7L);
        inventory.setSourceRevision(3L);
        inventory.setPolicy("{\"ksm\":{\"enabled\":true}}");
        inventory.setEffectivePolicy("{\"ksm\":{\"enabled\":true}}");
        inventory.setSource("Host");
        inventory.setAllowedActions(java.util.Arrays.asList("query", "apply", "pause"));
        java.util.Map<String, Long> revisions = new java.util.LinkedHashMap<>();
        revisions.put("Global:global", 3L);
        revisions.put("Cluster:234567890abcdef1234567890abcdef1", 4L);
        revisions.put("Host:" + inventory.getResourceUuid(), inventory.getRevision());
        inventory.setSourceRevisions(revisions);
        inventory.setFieldSources(java.util.Collections.singletonMap("ksm.enabled", "Host:" + inventory.getResourceUuid()));
        inventory.setFieldCapabilities(java.util.Collections.singletonMap("ksm.enabled",
                new MemoryFieldCapability(true, true, "Boolean", "", "Enables Kernel Same-page Merging")));
        inventory.setFieldModes(java.util.Collections.<String, String>emptyMap());
        return inventory;
    }

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
    private java.util.List<String> allowedActions;
    private java.util.List<String> preflightRequiredActions = new java.util.ArrayList<>();
    private java.util.Map<String, MemoryFieldCapability> fieldCapabilities;
    private String source;
    public java.util.List<String> getAllowedActions() { return allowedActions; }
    public void setAllowedActions(java.util.List<String> value) { allowedActions = value; }
    public java.util.List<String> getPreflightRequiredActions() { return preflightRequiredActions; }
    public void setPreflightRequiredActions(java.util.List<String> value) {
        preflightRequiredActions = value == null ? new java.util.ArrayList<>() : value;
    }
    public java.util.Map<String, MemoryFieldCapability> getFieldCapabilities() { return fieldCapabilities; }
    public void setFieldCapabilities(java.util.Map<String, MemoryFieldCapability> value) { fieldCapabilities = value; }
    public String getSource() { return source; }
    public void setSource(String value) { source = value; }
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

    /** Present only on Global policy reads while/after the first-install bootstrap exists. */
    private String bootstrapStatus;
    public String getBootstrapStatus() { return bootstrapStatus; }
    public void setBootstrapStatus(String value) { bootstrapStatus = value; }

    private String bootstrapReason;
    public String getBootstrapReason() { return bootstrapReason; }
    public void setBootstrapReason(String value) { bootstrapReason = value; }

}
