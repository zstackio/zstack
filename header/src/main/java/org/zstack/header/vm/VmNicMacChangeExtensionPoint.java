package org.zstack.header.vm;

import org.zstack.header.core.workflow.Flow;

public interface VmNicMacChangeExtensionPoint {
    String ROLLBACK_ERROR = "vmNicMacRollbackError";

    /**
     * Builds the remote MAC update step for the VM owner's serialized flow chain.
     * The caller commits the Cloud NIC MAC only after all extension flows succeed.
     * The returned flow must signal success after confirmed remote completion,
     * not merely request acceptance, and support compensation on chain failure.
     * Rollback restores the pre-operation remote state; if compensation cannot be
     * confirmed, it stores an {@link org.zstack.header.errorcode.ErrorCode} under
     * {@link #ROLLBACK_ERROR} in the shared flow data before completing rollback.
     * The caller returns that error in preference to the original failure.
     *
     * @param nic Cloud NIC inventory before the update
     * @param targetMac validated, lowercase MAC to apply remotely
     * @param reconcile true to repair remote drift using the current Cloud MAC;
     *                  false for a user update requiring the remote MAC to match
     *                  the original Cloud MAC
     * @return the remote update and compensation flow, or {@code null} when this
     *         extension does not manage the NIC; it does not commit the Cloud MAC
     */
    Flow updateVmNicMacFlow(VmNicInventory nic, String targetMac, boolean reconcile);
}
