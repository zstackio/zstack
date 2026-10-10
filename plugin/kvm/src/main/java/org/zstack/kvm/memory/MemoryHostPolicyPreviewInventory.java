package org.zstack.kvm.memory;

import java.util.List;

/** Read-only per-Host eligibility for a proposed policy change. */
public class MemoryHostPolicyPreviewInventory {
    public static MemoryHostPolicyPreviewInventory __example__() {
        MemoryHostPolicyPreviewInventory inventory = new MemoryHostPolicyPreviewInventory();
        inventory.setHostUuid("1234567890abcdef1234567890abcdef");
        inventory.setEligible(true);
        inventory.setBlockedFields(java.util.Collections.<MemoryBlockedPolicyFieldInventory>emptyList());
        return inventory;
    }

    private String hostUuid;
    private boolean eligible;
    private List<MemoryBlockedPolicyFieldInventory> blockedFields;

    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public boolean isEligible() { return eligible; }
    public void setEligible(boolean value) { eligible = value; }
    public List<MemoryBlockedPolicyFieldInventory> getBlockedFields() { return blockedFields; }
    public void setBlockedFields(List<MemoryBlockedPolicyFieldInventory> value) { blockedFields = value; }
}
