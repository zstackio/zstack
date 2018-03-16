package org.zstack.testlib.identity

import org.zstack.sdk.RoleInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HasSession
import org.zstack.testlib.Spec
import org.zstack.testlib.SpecID
import org.zstack.testlib.SpecMethod
import org.zstack.testlib.SpecParam

class RoleSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description

    RoleSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    RoleInventory inventory

    private List<String> policyNames = []

    @SpecMethod
    void usePolicy(String pname) {
        preCreate {
            addDependency(pname, PolicySpec.class)
        }

        policyNames.add(pname)
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        inventory = createRole {
            resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.sessionId = sessionId
        }

        postCreate {
            policyNames.each { pname ->
                def p = findSpec(pname, PolicySpec.class) as PolicySpec

                attachPolicyToRole {
                    roleUuid = inventory.uuid
                    policyUuid = p.inventory.uuid
                    delegate.sessionId = sessionId
                }
            }
        }

        return id(name, uuid)
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
