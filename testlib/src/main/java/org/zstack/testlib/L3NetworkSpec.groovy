package org.zstack.testlib

import org.zstack.sdk.L3NetworkInventory

class L3NetworkSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam
    boolean system = false

    L3NetworkInventory inventory

    L3NetworkSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createL3Network {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.sessionId = sessionId
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.system = system
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
        def spec = new NetworkServiceSpec(envSpec)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    IpRangeSpec ip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = IpRangeSpec.class) Closure c) {
        def spec = new IpRangeSpec(envSpec)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteL3Network {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
