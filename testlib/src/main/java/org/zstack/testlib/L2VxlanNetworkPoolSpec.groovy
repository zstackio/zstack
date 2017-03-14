package org.zstack.testlib

import org.zstack.sdk.L2NetworkInventory

/**
 * Created by weiwang on 15/03/2017.
 */
class L2VxlanNetworkPoolSpec extends L2NetworkSpec {

    L2VxlanNetworkPoolSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createL2VxlanNetworkPool {
            delegate.name = name
            delegate.description = description
            delegate.physicalInterface = physicalInterface
            delegate.resourceUuid = uuid
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
        } as L2NetworkInventory

        postCreate {
            inventory = queryL2VxlanNetworkPool {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
