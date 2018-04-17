package org.zstack.test.integration.identity.account

import org.zstack.identity.AccountManagerImpl
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.ZStackTest
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
        useSpring(ZStackTest.springSpec)
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
            testRenewSession()
            testRenewSessionFail()
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

    void testRenewSession() {
        accountInventory = createAccount {
            name = "test1"
            password = "password1"
        } as AccountInventory

        SessionInventory sess1 = logInByAccount {
            accountName = "test1"
            password = "password1"
        } as SessionInventory

        assert acntMgr.getSessionsCopy().get(sess1.uuid) != null

        SessionInventory sess2 = renewSession {
            sessionUuid = sess1.uuid
            duration = 31536000L
        }

        assert sess2.uuid == sess1.uuid
        assert sess2.accountUuid == sess1.accountUuid
        assert sess2.userUuid == sess1.userUuid
    }

    void testRenewSessionFail() {
        SessionInventory sess1 = logInByAccount {
            accountName = "test1"
            password = "password1"
        } as SessionInventory

        assert acntMgr.getSessionsCopy().get(sess1.uuid) != null

        logOut {
            sessionUuid = sess1.uuid
        }

        expect (AssertionError.class) {
            renewSession {
                sessionUuid = sess1.uuid
                duration = 31536000L
            }
        }
    }
}
