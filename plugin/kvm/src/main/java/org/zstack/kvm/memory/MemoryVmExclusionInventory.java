package org.zstack.kvm.memory;

/** Why migration retained non-participation; no live qualification is inferred. */
public class MemoryVmExclusionInventory {
    public static MemoryVmExclusionInventory __example__() {
        MemoryVmExclusionInventory inventory = new MemoryVmExclusionInventory();
        inventory.setSourceHostUuid("1234567890abcdef1234567890abcdef");
        inventory.setTargetHostUuid("abcdef1234567890abcdef1234567890");
        inventory.setSourceRevisions("{\"Host\":4}");
        inventory.setSourceInstanceGeneration("vm-generation-3");
        inventory.setRetained(true);
        return inventory;
    }

    private String sourceHostUuid;
    private String targetHostUuid;
    private String sourceRevisions;
    private String sourceInstanceGeneration;
    private boolean retained;
    public String getSourceHostUuid() { return sourceHostUuid; }
    public void setSourceHostUuid(String value) { sourceHostUuid = value; }
    public String getTargetHostUuid() { return targetHostUuid; }
    public void setTargetHostUuid(String value) { targetHostUuid = value; }
    public String getSourceRevisions() { return sourceRevisions; }
    public void setSourceRevisions(String value) { sourceRevisions = value; }
    public String getSourceInstanceGeneration() { return sourceInstanceGeneration; }
    public void setSourceInstanceGeneration(String value) { sourceInstanceGeneration = value; }
    public boolean isRetained() { return retained; }
    public void setRetained(boolean value) { retained = value; }
}
