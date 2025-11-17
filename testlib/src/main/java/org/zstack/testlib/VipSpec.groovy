package org.zstack.testlib

import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmNicInventory

/**
 * Created by shixin.ruan on 2025/12/10.
 */
class VipSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam(required = true)
    String l3NetworkUuid
    @SpecParam
    String description
    @SpecParam
    String requiredIp

    VipInventory inventory

    VipSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createVip {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.l3NetworkUuid = l3NetworkUuid
            delegate.description = description
            delegate.requiredIp = requiredIp
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
        }

        postCreate {
            inventory = queryVip {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteVip {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
