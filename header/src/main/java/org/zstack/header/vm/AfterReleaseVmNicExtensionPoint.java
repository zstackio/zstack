package org.zstack.header.vm;

import org.zstack.header.core.Completion;

/**
 * Extension point called after a VmNic has been deleted from the database.
 * Implementations perform post-deletion cleanup (e.g., deleting SDN segment ports).
 * Cloud DB deletion must succeed before this extension point is invoked.
 */
public interface AfterReleaseVmNicExtensionPoint {
    void afterReleaseVmNic(VmNicInventory nic, Completion completion);
}
