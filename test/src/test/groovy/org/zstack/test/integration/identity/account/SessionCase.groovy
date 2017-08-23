package org.zstack.test.integration.identity.account

import org.zstack.core.db.SQL
import org.zstack.header.identity.SessionVO
import org.zstack.identity.AccountManagerImpl
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class SessionCase extends SubCase {
    EnvSpec env
    AccountInventory accountInventory
    AccountManagerImpl acntMgr

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(IdentityTest.springSpec)
    }

    @Override
    void environment() {
        env = env {}
    }

    @Override
    void test() {
        env.create {
            acntMgr = bean(AccountManagerImpl.class)
            accountInventory = createAccount {
                name = "test"
                password = "password"
            } as AccountInventory

            testSession()
        }
    }

    void testSession() {
        SessionInventory sessionInventory = logInByAccount {
            accountName = "test"
            password = "password"
        } as SessionInventory

        assert acntMgr.getSessionsCopy().get(sessionInventory.uuid) != null

        deleteAccount {
            uuid = accountInventory.uuid
        }

        retryInSecs(2){
            assert acntMgr.getSessionsCopy().get(sessionInventory.uuid) == null
        }
    }
}
