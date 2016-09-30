package org.zstack.test.compute.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceStartNewCreatedVmExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class VmInstanceStartNewCreatedVmExtension implements VmInstanceStartNewCreatedVmExtensionPoint {
    private CLogger logger = Utils.getLogger(VmInstanceStartNewCreatedVmExtensionPoint.class);
    boolean preventStart = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    boolean failedCalled = false;
    String expectedUuid;

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        if (preventStart) {
            return "Prevent starting vm on purpose";
        } else {
            return null;
        }
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {
        beforeCalled = true;
    }

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {
        afterCalled = true;
    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {
        failedCalled = true;
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
