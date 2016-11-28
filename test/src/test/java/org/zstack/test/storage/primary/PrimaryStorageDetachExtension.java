package org.zstack.test.storage.primary;

import org.zstack.header.storage.primary.PrimaryStorageDetachExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageException;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class PrimaryStorageDetachExtension implements PrimaryStorageDetachExtensionPoint {
    CLogger logger = Utils.getLogger(PrimaryStorageDetachExtension.class);
    boolean preventDetach = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    String expectedClusterUuid;
    String expectedPrimaryStorageUuid;

    @Override
    public void preDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) throws PrimaryStorageException {
        if (this.preventDetach) {
            throw new PrimaryStorageException("Prevent detaching primary storage on purpose");
        }
    }

    @Override
    public void beforeDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        if (inventory.getUuid().equals(this.expectedPrimaryStorageUuid) && clusterUuid.equals(this.expectedClusterUuid)) {
            this.beforeCalled = true;
        } else {
            String err = String.format("beforeDetachPrimaryStorage: expected primaryStorageUuid:%s clusterUuid:%s but got primaryStorageUuid: %s clusterUuid: %s", this.expectedPrimaryStorageUuid, this.expectedClusterUuid, inventory.getUuid(), clusterUuid);
            logger.warn(err);
        }
    }

    @Override
    public void failToDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        // TODO Auto-generated method stub

    }

    @Override
    public void afterDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        if (inventory.getUuid().equals(this.expectedPrimaryStorageUuid) && clusterUuid.equals(this.expectedClusterUuid)) {
            this.afterCalled = true;
        } else {
            String err = String.format("afterDetachPrimaryStorage: expected primaryStorageUuid:%s clusterUuid:%s" +
                            " but got primaryStorageUuid: %s clusterUuid: %s",
                    this.expectedPrimaryStorageUuid, this.expectedClusterUuid, inventory.getUuid(), clusterUuid);
            logger.warn(err);
        }
    }

    public CLogger getLogger() {
        return logger;
    }

    public void setLogger(CLogger logger) {
        this.logger = logger;
    }

    public boolean isPreventDetach() {
        return preventDetach;
    }

    public void setPreventDetach(boolean preventDetach) {
        this.preventDetach = preventDetach;
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

    public String getExpectedClusterUuid() {
        return expectedClusterUuid;
    }

    public void setExpectedClusterUuid(String expectedClusterUuid) {
        this.expectedClusterUuid = expectedClusterUuid;
    }

    public String getExpectedPrimaryStorageUuid() {
        return expectedPrimaryStorageUuid;
    }

    public void setExpectedPrimaryStorageUuid(String expectedPrimaryStorageUuid) {
        this.expectedPrimaryStorageUuid = expectedPrimaryStorageUuid;
    }
}
