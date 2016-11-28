package org.zstack.test.storage.primary;

import org.zstack.header.storage.primary.PrimaryStorageAttachExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageException;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class PrimaryStorageAttachExtension implements PrimaryStorageAttachExtensionPoint {
    CLogger logger = Utils.getLogger(PrimaryStorageAttachExtension.class);
    boolean preventAttach = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    String expectedPrimaryStorageUuid;
    String expectedClusterUuid;

    @Override
    public void preAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) throws PrimaryStorageException {
        if (this.preventAttach) {
            throw new PrimaryStorageException("Prevent attaching primary storage on purpose");
        }
    }

    @Override
    public void beforeAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        if (inventory.getUuid().equals(this.expectedPrimaryStorageUuid) && clusterUuid.equals(this.expectedClusterUuid)) {
            this.beforeCalled = true;
        } else {
            String err = String.format("beforeAttachPrimaryStorage: expected primaryStorageUuid: %s clusterUuid: %s but got primaryStorageUuid: %s clusterUuid: %s", this.expectedPrimaryStorageUuid, this.expectedClusterUuid, inventory.getUuid(), clusterUuid);
            logger.warn(err);
        }
    }

    @Override
    public void failToAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
    }

    @Override
    public void afterAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        if (inventory.getUuid().equals(this.expectedPrimaryStorageUuid) && clusterUuid.equals(this.expectedClusterUuid)) {
            this.afterCalled = true;
        } else {
            String err = String.format("afterAttachPrimaryStorage: expected primaryStorageUuid: %s clusterUuid: %s but got primaryStorageUuid: %s clusterUuid: %s", this.expectedPrimaryStorageUuid, this.expectedClusterUuid, inventory.getUuid(), clusterUuid);
            logger.warn(err);
        }
    }

    public boolean isPreventAttach() {
        return preventAttach;
    }

    public void setPreventAttach(boolean preventAttach) {
        this.preventAttach = preventAttach;
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

    public String getExpectedPrimaryStorageUuid() {
        return expectedPrimaryStorageUuid;
    }

    public void setExpectedPrimaryStorageUuid(String expectedPrimaryStorageUuid) {
        this.expectedPrimaryStorageUuid = expectedPrimaryStorageUuid;
    }

    public String getExpectedClusterUuid() {
        return expectedClusterUuid;
    }

    public void setExpectedClusterUuid(String expectedClusterUuid) {
        this.expectedClusterUuid = expectedClusterUuid;
    }
}
