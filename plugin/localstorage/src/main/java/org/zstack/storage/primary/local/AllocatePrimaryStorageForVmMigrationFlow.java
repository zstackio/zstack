package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;

/**
 * Created by frank on 10/24/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AllocatePrimaryStorageForVmMigrationFlow  extends AbstractHostAllocatorFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

    @Override
    public void allocate() {
        if (amITheFirstFlow()) {
            throw new CloudRuntimeException("AllocatePrimaryStorageForVmMigrationFlow cannot be the first flow in the chain");
        }
    }
}
