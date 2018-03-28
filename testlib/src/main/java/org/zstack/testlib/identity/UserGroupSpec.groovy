package org.zstack.testlib.identity

import org.zstack.sdk.PolicyInventory
import org.zstack.sdk.UserGroupInventory
import org.zstack.sdk.UserInventory
import org.zstack.testlib.*

class UserGroupSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description

    UserGroupInventory inventory

    UserGroupSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    private List<String> policyNames = []
    private List<String> userNames = []
    private List<String> roleNames = []

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

    void addUser(String uname) {
        preCreate {
            addDependency(uname, UserSpec.class)
        }

        userNames.add(uname)
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        inventory = createUserGroup {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.sessionId = sessionId
        }

        postCreate {
            policyNames.each { pname ->
                PolicyInventory pinv = findSpec(pname, PolicySpec.class).inventory

                attachPolicyToUserGroup {
                    groupUuid = inventory.uuid
                    policyUuid = pinv.uuid
                    delegate.sessionId = sessionId
                }
            }

            userNames.each { uname ->
                UserInventory uinv = findSpec(uname, UserSpec.class).inventory

                addUserToGroup {
                    userUuid = uinv.uuid
                    groupUuid = inventory.uuid
                    delegate.sessionId = sessionId
                }
            }

            roleNames.each { rname ->
                def inv = findSpec(rname, RoleSpec.class).inventory
                attachRoleToUserGroup {
                    roleUuid = inv.uuid
                    groupUuid = inventory.uuid
                    delegate.sessionId = sessionId
                }
            }
        }

        return id(name, uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteUserGroup {
                uuid = inventory.uuid
                delegate.sessionId = sessionId
            }
        }
    }
}
