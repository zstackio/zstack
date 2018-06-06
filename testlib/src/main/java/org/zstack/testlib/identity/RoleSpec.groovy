package org.zstack.testlib.identity

import org.zstack.sdk.identity.role.RoleInventory
import org.zstack.testlib.*

class RoleSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description

    RoleSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    RoleInventory inventory

    @Override
    SpecID create(String uuid, String sessionId) {
        inventory = createRole {
            resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.sessionId = sessionId
        }

        return id(name, uuid)
    }

    void policy(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value=PolicySpec.class) Closure c) {
        PolicySpec spec = new PolicySpec(envSpec)
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = spec
        c()
        addChild(spec)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteRole {
                uuid = inventory.uuid
                delegate.sessionId = sessionId
            }
        }
    }
}
