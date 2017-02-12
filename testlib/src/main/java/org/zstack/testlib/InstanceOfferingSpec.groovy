package org.zstack.testlib

import org.zstack.sdk.InstanceOfferingInventory

/**
 * Created by xing5 on 2017/2/15.
 */
class InstanceOfferingSpec implements Spec, HasSession {
    String name
    String description
    Long memory
    Long cpu
    String allocatorStrategy

    public InstanceOfferingInventory inventory

    SpecID create(String uuid, String sessionId) {
        inventory = createInstanceOffering {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.memorySize = memory
            delegate.cpuNum = cpu
            delegate.allocatorStrategy = allocatorStrategy
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
        }

        postCreate {
            inventory = queryInstanceOffering {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
