package org.zstack.testlib.identity

import org.zstack.sdk.PolicyInventory
import org.zstack.sdk.UserInventory
import org.zstack.testlib.*

class UserSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam
    String password

    UserInventory inventory

    private List<String> policyNames = []
    private List<String> roleNames = []

    UserSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    void usePolicy(String pname) {
        preCreate {
            addDependency(pname, PolicySpec.class)
        }

        policyNames.add(pname)
    }


    void useRole(String rname) {
        preCreate {
            addDependency(rname, RoleSpec.class)
        }

        roleNames.add(rname)
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

            roleNames.each { rname ->
                def inv = findSpec(rname, RoleSpec.class).inventory
                attachRoleToUser {
                    roleUuid = inv.uuid
                    userUuid = inventory.uuid
                    delegate.sessionId = sessionId
                }
            }
        }

        return id(name, uuid)
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
