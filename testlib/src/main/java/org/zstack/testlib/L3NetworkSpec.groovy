package org.zstack.testlib

import org.zstack.sdk.L3NetworkInventory

class L3NetworkSpec implements Spec, HasSession {
    String name
    String description

    L3NetworkInventory inventory

    SpecID create(String uuid, String sessionId) {
        inventory = createL3Network {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.sessionId = sessionId
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.l2NetworkUuid = (parent as L2NetworkSpec).inventory.uuid
        }

        postCreate {
            inventory = queryL3Network {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    NetworkServiceSpec service(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = NetworkServiceSpec.class) Closure c) {
        def spec = new NetworkServiceSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    IpRangeSpec ip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = IpRangeSpec.class) Closure c) {
        def spec = new IpRangeSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }
}
