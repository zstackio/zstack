package org.zstack.kvm.memory;

/** Effective policy pair used to preview a parent-scope change against a Host's explicit overrides. */
final class MemoryHostPolicyPreviewSnapshot {
    final String before;
    final String after;
    final MemoryStateVO state;
    MemoryHostPolicyPreviewSnapshot(String before, String after, MemoryStateVO state) {
        this.before = before; this.after = after; this.state = state;
    }
}
