package org.zstack.testlib

import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory

/**
 * Created by xing5 on 2017/2/15.
 */
class AccountSpec extends Spec {
    @SpecParam(required = true)
    String name
    @SpecParam(required = true)
    String password

    AccountInventory inventory
    SessionInventory session

    AccountSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createAccount {
            delegate.resourceUuid = uuid
            delegate.sessionId = sessionId
            delegate.name = name
            delegate.password = password
        }

        session = logInByAccount {
            delegate.accountName = name
            delegate.password = password
        } as SessionInventory

        postCreate {
            inventory = queryAccount {
                conditions = ["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    Closure use() {
        String session = null

        return {
            if (session == null) {
                SessionInventory s = logInByAccount {
                    delegate.accountName = name
                    delegate.password = password
                } as SessionInventory

                session = s.uuid
            }

            return session
        }
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteAccount {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
