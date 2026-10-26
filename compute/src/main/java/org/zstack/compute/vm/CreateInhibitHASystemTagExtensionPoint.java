package org.zstack.compute.vm;

/** Restores HA inhibition when an explicit stop or deletion enters the VM queue. */
public interface CreateInhibitHASystemTagExtensionPoint extends DeleteInhibitHASystemTagExtensionPoint {
    /**
     * Ensures HA inhibition when an explicit stop or deletion starts in the VM queue.
     * Implementations must be idempotent and create a tag only when the VM's HA
     * policy requires inhibition and no tag already exists.
     *
     * @param vmInstanceUuid VM whose stop or deletion is being executed
     * @return true only if this invocation created the tag; false for an existing
     *         tag (including a concurrent creation) or an inapplicable HA policy.
     *         On failure with the VM still Running, the caller may use
     *         {@link #deleteInhibitHaSystemTag(String)} to undo a true result.
     */
    boolean createInhibitHaSystemTag(String vmInstanceUuid);
}
