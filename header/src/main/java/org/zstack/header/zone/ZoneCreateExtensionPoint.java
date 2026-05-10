package org.zstack.header.zone;

public interface ZoneCreateExtensionPoint {
    void afterCreateZone(ZoneInventory inventory);
}
