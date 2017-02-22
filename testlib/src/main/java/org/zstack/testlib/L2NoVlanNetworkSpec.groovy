package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/15.
 */
class L2NoVlanNetworkSpec extends L2NetworkSpec {
    L2NoVlanNetworkSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createL2NoVlanNetwork {
            delegate.name = name
            delegate.description = description
            delegate.resourceUuid = uuid
            delegate.sessionId = sessionId
            delegate.physicalInterface = physicalInterface
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
        }

        postCreate {
            inventory = queryL2Network {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
