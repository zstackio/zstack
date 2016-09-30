package org.zstack.test.compute.host;

import org.zstack.header.host.HostDeleteExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class DeletHostExtension implements HostDeleteExtensionPoint {
    CLogger logger = Utils.getLogger(DeletHostExtension.class);
    private boolean preventDelete = false;
    private boolean beforeCalled = false;
    private boolean afterCalled = false;
    private String expectedHostUuid;

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {
        if (this.preventDelete) {
            throw new HostException("Prevent deleting host on purpose");
        }
    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {
        if (inventory.getUuid().equals(this.expectedHostUuid)) {
            this.beforeCalled = true;
        } else {
            String err = String.format("beforeDeleteHost: expected host uuid:%s but %s got", this.expectedHostUuid, inventory.getUuid());
            logger.warn(err);
        }
    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
        if (inventory.getUuid().equals(this.expectedHostUuid)) {
            this.afterCalled = true;
        } else {
            String err = String.format("afterDeleteHost: expected host uuid:%s but %s got", this.expectedHostUuid, inventory.getUuid());
            logger.warn(err);
        }
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

    public String getExpectedHostUuid() {
        return expectedHostUuid;
    }

    public void setExpectedHostUuid(String expectedHostUuid) {
        this.expectedHostUuid = expectedHostUuid;
    }
}
