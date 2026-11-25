package org.zstack.kvm.memory;

/** A swap disk is chosen from the Host's own candidate list, never by a path. */
public final class MemorySwapDiskRules {
    public static final String PREPARE = "prepareSwapDisk";
    public static final String DISABLE = "disableSwapDisk";
    /** mkswap, swapon --discard of a whole disk and swapoff of what it holds can take minutes. */
    static final int AGENT_SECONDS = 1800;
    private MemorySwapDiskRules() { }
}
