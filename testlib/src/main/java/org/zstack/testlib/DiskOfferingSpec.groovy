package org.zstack.testlib

import org.zstack.sdk.DiskOfferingInventory

/**
 * Created by xing5 on 2017/2/16.
 */
class DiskOfferingSpec implements Spec, HasSession {
    String name
    String description
    Long diskSize
    String allocatorStrategy

    DiskOfferingInventory inventory

    SpecID create(String uuid, String sessionId) {
        inventory = createDiskOffering {
            delegate.name = name
            delegate.description = description
            delegate.diskSize = diskSize
            delegate.allocationStrategy = allocatorStrategy
        }

        postCreate {
            inventory = queryDiskOffering {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteDiskOffering {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
