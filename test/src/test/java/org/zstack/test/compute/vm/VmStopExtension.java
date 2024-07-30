package org.zstack.test.compute.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceStopExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class VmStopExtension implements VmInstanceStopExtensionPoint {
    CLogger logger = Utils.getLogger(VmStopExtension.class);
    boolean preventStop = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    boolean failedCalled = false;
    String expectedUuid;

    @Override
    public String preStopVm(VmInstanceInventory inv) {
        if (preventStop) {
            return "Prevent stopping vm on purpose";
        } else {
            return null;
        }
    }

    @Override
    public void beforeStopVm(VmInstanceInventory inv) {
        if (inv.getUuid().equals(expectedUuid)) {
            beforeCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }
    }

    @Override
    public void afterStopVm(VmInstanceInventory inv) {
        if (inv.getUuid().equals(expectedUuid)) {
            afterCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }

    }

    @Override
    public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) {
        if (inv.getUuid().equals(expectedUuid)) {
            failedCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }
    }

    public boolean isPreventStop() {
        return preventStop;
    }

    public void setPreventStop(boolean preventStop) {
        this.preventStop = preventStop;
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

    public boolean isFailedCalled() {
        return failedCalled;
    }

    public void setFailedCalled(boolean failedCalled) {
        this.failedCalled = failedCalled;
    }

    public String getExpectedUuid() {
        return expectedUuid;
    }

    public void setExpectedUuid(String expectedUuid) {
        this.expectedUuid = expectedUuid;
    }
}
