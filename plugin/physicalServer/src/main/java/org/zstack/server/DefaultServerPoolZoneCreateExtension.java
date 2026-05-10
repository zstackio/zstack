package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.zone.ZoneCreateExtensionPoint;
import org.zstack.header.zone.ZoneInventory;

public class DefaultServerPoolZoneCreateExtension implements ZoneCreateExtensionPoint {
    @Autowired
    private DefaultServerPoolFactory defaultServerPoolFactory;

    @Override
    public void afterCreateZone(ZoneInventory inventory) {
        DefaultServerPoolCreationPolicy policy = DefaultServerPoolCreationPolicy.valueOf(
                PhysicalServerGlobalConfig.DEFAULT_SERVER_POOL_CREATION_POLICY.value(String.class));
        if (policy != DefaultServerPoolCreationPolicy.OnZoneCreate) {
            return;
        }

        defaultServerPoolFactory.ensureDefaultPool(inventory.getUuid());
    }
}
