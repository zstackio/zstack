package org.zstack.testlib.identity

import org.zstack.sdk.PolicyInventory
import org.zstack.sdk.UserInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HasSession
import org.zstack.testlib.Spec
import org.zstack.testlib.SpecID
import org.zstack.testlib.SpecParam

class UserSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam
    String password

    UserInventory inventory

    private List<String> policyNames = []

    UserSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    void usePolicy(String pname) {
        preCreate {
            addDependency(pname, PolicySpec.class)
        }

        policyNames.add(pname)
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        inventory = createUser {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.password = password
            delegate.description = description
            delegate.sessionId = sessionId
        }

        postCreate {
            policyNames.each { pname ->
                PolicyInventory inv = findSpec(pname, PolicySpec.class).inventory
                attachPolicyToUser {
                    policyUuid = inv.uuid
                    userUuid = inventory.uuid
                    delegate.sessionId = sessionId
                }
            }
        }

        return new SpecID(name:name, uuid:uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteUser {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }
        }
    }
}
