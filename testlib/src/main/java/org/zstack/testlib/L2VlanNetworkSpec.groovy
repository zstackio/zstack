package org.zstack.testlib

import org.zstack.sdk.L2NetworkInventory

/**
 * Created by xing5 on 2017/2/15.
 */
class L2VlanNetworkSpec extends L2NetworkSpec {
    @SpecParam(required = true)
    Integer vlan

    L2VlanNetworkSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createL2VlanNetwork {
            delegate.name = name
            delegate.description = description
            delegate.physicalInterface = physicalInterface
            delegate.resourceUuid = uuid
            delegate.vlan = vlan
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
        } as L2NetworkInventory

        postCreate {
            inventory = queryL2VlanNetwork {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
