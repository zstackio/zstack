package org.zstack.test.compute.cluster;

import org.zstack.header.cluster.ClusterDeleteExtensionPoint;
import org.zstack.header.cluster.ClusterException;
import org.zstack.header.cluster.ClusterInventory;

public class ClusterDeleteExtension implements ClusterDeleteExtensionPoint {
    private boolean preventDelete = false;
    private boolean beforeCalled = false;
    private boolean afterCalled = false;

    @Override
    public void preDeleteCluster(ClusterInventory inventory) throws ClusterException {
        if (this.preventDelete) {
            throw new ClusterException("Prevent deleting cluster on purpose");
        }
    }

    @Override
    public void beforeDeleteCluster(ClusterInventory inventory) {
        this.beforeCalled = true;
    }

    @Override
    public void afterDeleteCluster(ClusterInventory inventory) {
        this.afterCalled = true;
    }

    public boolean isPreventDelete() {
        return preventDelete;
    }

    public void setPreventDelete(boolean preventDelete) {
        this.preventDelete = preventDelete;
    }

    public boolean isBeforeCalled() {
        return beforeCalled;
    }

    public void setBeforeCalled(boolean beforeCalled) {
        this.beforeCalled = beforeCalled;
    }

    public boolean isAfterCalled() {
        return afterCalled;
    }

    public void setAfterCalled(boolean afterCalled) {
        this.afterCalled = afterCalled;
    }
}
