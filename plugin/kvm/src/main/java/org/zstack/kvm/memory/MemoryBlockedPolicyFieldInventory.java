package org.zstack.kvm.memory;

/** A policy field that cannot be changed on a Host using its current capability sample. */
public class MemoryBlockedPolicyFieldInventory {
    private String field;
    private String reasonCode;
    public MemoryBlockedPolicyFieldInventory() { }
    public MemoryBlockedPolicyFieldInventory(String field, String reasonCode) {
        this.field = field; this.reasonCode = reasonCode;
    }
    public String getField() { return field; }
    public void setField(String value) { field = value; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String value) { reasonCode = value; }
    public static MemoryBlockedPolicyFieldInventory __example__() {
        return new MemoryBlockedPolicyFieldInventory("zram.writeback.enabled", "BACKEND_NOT_QUALIFIED");
    }
}
