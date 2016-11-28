package org.zstack.test.network;

import org.zstack.header.network.l2.L2NetworkDeleteExtensionPoint;
import org.zstack.header.network.l2.L2NetworkException;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class L2NetworkDeleteExtension implements L2NetworkDeleteExtensionPoint {
    CLogger logger = Utils.getLogger(L2NetworkDeleteExtension.class);
    boolean preventDelete = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    String excpectedUuid;

    @Override
    public void preDeleteL2Network(L2NetworkInventory inventory) throws L2NetworkException {
        if (preventDelete) {
            throw new L2NetworkException("Prevent deleting on purpose");
        }
    }

    @Override
    public void beforeDeleteL2Network(L2NetworkInventory inventory) {
        if (inventory.getUuid().equals(excpectedUuid)) {
            beforeCalled = true;
        } else {
            String err = String.format("beforeDeleteL2Network: expected uuid: %s but got :%s", excpectedUuid, inventory.getUuid());
            logger.debug(err);
        }
    }

    @Override
    public void afterDeleteL2Network(L2NetworkInventory inventory) {
        if (inventory.getUuid().equals(excpectedUuid)) {
            afterCalled = true;
        } else {
            String err = String.format("afterDeleteL2Network: expected uuid: %s but got :%s", excpectedUuid, inventory.getUuid());
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
