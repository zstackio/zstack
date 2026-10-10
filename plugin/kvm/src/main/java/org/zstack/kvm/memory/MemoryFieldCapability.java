package org.zstack.kvm.memory;

/** Presentation metadata, never an authorization token. */
public class MemoryFieldCapability {
    public boolean readable = true;
    public boolean writable;
    public boolean basicUi;
    public String type;
    public String unit;
    public String effect;
    public String reasonCode;
    public MemoryFieldCapability(boolean writable, boolean basicUi, String type, String unit, String effect) {
        this.writable = writable; this.basicUi = basicUi; this.type = type; this.unit = unit; this.effect = effect;
    }

    public static MemoryFieldCapability __example__() {
        MemoryFieldCapability capability = new MemoryFieldCapability(true, true, "Boolean", "", "Enables KSM");
        capability.reasonCode = "SUPPORTED";
        return capability;
    }
}
