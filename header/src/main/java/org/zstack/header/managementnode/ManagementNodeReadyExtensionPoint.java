package org.zstack.header.managementnode;

/**
 * Created by frank on 11/9/2015.
 */
public interface ManagementNodeReadyExtensionPoint {
    /**
     * Called once during management node startup.
     * Start business tasks requiring resource routing or HTTP callbacks here.
     */
    void managementNodeReady();
}
