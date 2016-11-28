package org.zstack.test.compute.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceDestroyExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class VmDestroyExtension implements VmInstanceDestroyExtensionPoint {
    CLogger logger = Utils.getLogger(VmDestroyExtension.class);
    boolean preventDestroy = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    boolean failedCalled = false;
    String expectedUuid;

    @Override
    public String preDestroyVm(VmInstanceInventory inv) {
        if (preventDestroy) {
            return "Prevent destroying vm on purpose";
        }
        return null;
    }

    @Override
    public void beforeDestroyVm(VmInstanceInventory inv) {
        if (inv.getUuid().equals(expectedUuid)) {
            beforeCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }
    }

    @Override
    public void afterDestroyVm(VmInstanceInventory inv) {
        if (inv.getUuid().equals(expectedUuid)) {
            afterCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }

    }

    @Override
    public void failedToDestroyVm(VmInstanceInventory inv, ErrorCode reason) {
        if (inv.getUuid().equals(expectedUuid)) {
            failedCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }
    }

    public boolean isPreventDestroy() {
        return preventDestroy;
    }

    public void setPreventDestroy(boolean preventDestroy) {
        this.preventDestroy = preventDestroy;
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
