package org.zstack.test.compute.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class VmMigrateExtension implements VmInstanceMigrateExtensionPoint {
    CLogger logger = Utils.getLogger(VmMigrateExtension.class);
    boolean preventMigrate = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    boolean failedCalled = false;
    String expectedUuid;

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String huuid) {
        if (preventMigrate) {
            throw new CloudRuntimeException("on purpose");
        }
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String huuid) {
        if (inv.getUuid().equals(expectedUuid)) {
            beforeCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        if (inv.getUuid().equals(expectedUuid)) {
            afterCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String huuid, ErrorCode reason) {
        if (inv.getUuid().equals(expectedUuid)) {
            failedCalled = true;
        } else {
            logger.debug(String.format("Expected uuid: %s but got %s", expectedUuid, inv.getUuid()));
        }
    }

    public boolean isPreventMigrate() {
        return preventMigrate;
    }

    public void setPreventMigrate(boolean preventMigrate) {
        this.preventMigrate = preventMigrate;
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
