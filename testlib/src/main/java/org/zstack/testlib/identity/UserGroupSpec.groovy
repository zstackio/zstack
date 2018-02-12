package org.zstack.testlib.identity

import org.zstack.sdk.PolicyInventory
import org.zstack.sdk.UserGroupInventory
import org.zstack.sdk.UserInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HasSession
import org.zstack.testlib.Spec
import org.zstack.testlib.SpecID
import org.zstack.testlib.SpecParam

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

    void usePolicy(String pname) {
        preCreate {
            addDependency(pname, PolicySpec.class)
        }

        policyNames.add(pname)
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
        }

        return new SpecID(name:name, uuid:uuid)
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
