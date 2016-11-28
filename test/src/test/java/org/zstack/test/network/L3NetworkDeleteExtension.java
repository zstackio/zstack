package org.zstack.test.network;

import org.zstack.header.network.l3.L3NetworkDeleteExtensionPoint;
import org.zstack.header.network.l3.L3NetworkException;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class L3NetworkDeleteExtension implements L3NetworkDeleteExtensionPoint {
    CLogger logger = Utils.getLogger(L3NetworkDeleteExtension.class);
    boolean preventDelete = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    String excpectedUuid;

    @Override
    public String preDeleteL3Network(L3NetworkInventory inventory) throws L3NetworkException {
        if (preventDelete) {
            throw new L3NetworkException("Prevent deleting on purpose");
        } else {
            return null;
        }
    }

    @Override
    public void beforeDeleteL3Network(L3NetworkInventory inventory) {
        if (inventory.getUuid().equals(excpectedUuid)) {
            beforeCalled = true;
        } else {
            String err = String.format("beforeDeleteL3Network: expected uuid:%s but got %s", excpectedUuid, inventory.getUuid());
            logger.debug(err);
        }
    }

    @Override
    public void afterDeleteL3Network(L3NetworkInventory inventory) {
        if (inventory.getUuid().equals(excpectedUuid)) {
            afterCalled = true;
        } else {
            String err = String.format("afterCalled: expected uuid:%s but got %s", excpectedUuid, inventory.getUuid());
            logger.debug(err);
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

    public String getExcpectedUuid() {
        return excpectedUuid;
    }

    public void setExcpectedUuid(String excpectedUuid) {
        this.excpectedUuid = excpectedUuid;
    }
}
