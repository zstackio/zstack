package org.zstack.test.compute.zone;

import org.zstack.header.zone.ZoneDeleteExtensionPoint;
import org.zstack.header.zone.ZoneException;
import org.zstack.header.zone.ZoneInventory;

public class PreventZoneDeleteExtensionPoint implements ZoneDeleteExtensionPoint {
    private boolean beforeCalled = false;
    private boolean afterCalled = false;

    @Override
    public void preDeleteZone(ZoneInventory inventory) throws ZoneException {
        throw new ZoneException("Prevent deleting zone on purpose");
    }

    @Override
    public void beforeDeleteZone(ZoneInventory inventory) {
        this.beforeCalled = true;
    }

    @Override
    public void afterDeleteZone(ZoneInventory inventory) {
        this.afterCalled = true;
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
}
