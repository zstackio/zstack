package org.zstack.test.compute.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceStartExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class VmStartExtension implements VmInstanceStartExtensionPoint {
    CLogger logger = Utils.getLogger(VmStartExtension.class);
    boolean preventStart = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    boolean failedCalled = false;
    String expectedUuid;

    @Override
    public String preStartVm(VmInstanceInventory inv) {
        if (preventStart) {
            return "Prevent starting vm on purpose";
        }
        return null;
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {
        if (inv.getUuid().equals(expectedUuid)) {
            beforeCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }
    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {
        if (inv.getUuid().equals(expectedUuid)) {
            afterCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }
    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {
        if (inv.getUuid().equals(expectedUuid)) {
            failedCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }
    }

    public boolean isPreventStart() {
        return preventStart;
    }

    public void setPreventStart(boolean preventStart) {
        this.preventStart = preventStart;
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
