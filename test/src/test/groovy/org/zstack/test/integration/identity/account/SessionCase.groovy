package org.zstack.test.integration.identity.account

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.DatabaseFacadeImpl
import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.cloudbus.EventFacade
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.IdentityCanonicalEvents
import org.zstack.identity.AccountManagerImpl
import org.zstack.identity.IdentityGlobalConfig
import org.zstack.identity.Session
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.sdk.LogInByAccountAction
import org.zstack.header.identity.IdentityErrors

import javax.persistence.Query
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class SessionCase extends SubCase {
    EnvSpec env
    AccountInventory accountInventory
    AccountManagerImpl acntMgr
    DatabaseFacadeImpl dbf

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
            dbf = bean(DatabaseFacadeImpl.class)
            acntMgr = bean(AccountManagerImpl.class)
            accountInventory = createAccount {
                name = "test"
                password = "password"
            } as AccountInventory

            testSession()
            testRenewSession()
            testRenewSessionFail()
            testInvalidSession()
            testForceLogoutEventEvictsSessionCache()
            testLogoutBroadcastsForceLogoutEvent()
            testValidateSessionApi()
            testMaxCurrentSessionExceeded()
        }
    }

    void testForceLogoutEventEvictsSessionCache() {
        AccountInventory account = createAccount {
            name = "force-logout-cache"
            password = "password"
        } as AccountInventory

        SessionInventory session = logInByAccount {
            accountName = account.name
            password = "password"
        } as SessionInventory

        assert Session.getSessionsCopy().containsKey(session.uuid)

        IdentityCanonicalEvents.SessionForceLogoutData data = new IdentityCanonicalEvents.SessionForceLogoutData()
        data.sessionUuid = session.uuid
        data.accountUuid = session.accountUuid
        data.userUuid = session.userUuid
        bean(EventFacade.class).fire(IdentityCanonicalEvents.SESSION_FORCE_LOGOUT_PATH, data)

        retryInSecs {
            assert !Session.getSessionsCopy().containsKey(session.uuid)
        }

        Session.logout(session.uuid)
        deleteAccount {
            uuid = account.uuid
        }
    }

    void testLogoutBroadcastsForceLogoutEvent() {
        AccountInventory account = createAccount {
            name = "force-logout-broadcast"
            password = "password"
        } as AccountInventory

        SessionInventory session = logInByAccount {
            accountName = account.name
            password = "password"
        } as SessionInventory

        EventFacade evtf = bean(EventFacade.class)
        AtomicReference<IdentityCanonicalEvents.SessionForceLogoutData> received = new AtomicReference<>()
        EventCallback callback = new EventCallback() {
            @Override
            void run(Map tokens, Object eventData) {
                IdentityCanonicalEvents.SessionForceLogoutData data =
                        (IdentityCanonicalEvents.SessionForceLogoutData) eventData
                if (data.sessionUuid == session.uuid) {
                    received.set(data)
                }
            }
        }
        evtf.onLocal(IdentityCanonicalEvents.SESSION_FORCE_LOGOUT_PATH, callback)

        try {
            Session.logout(session.uuid)
            retryInSecs {
                assert received.get() != null
                assert received.get().accountUuid == session.accountUuid
                assert received.get().userUuid == session.userUuid
            }
        } finally {
            evtf.off(callback)
        }

        deleteAccount {
            uuid = account.uuid
        }
    }

    void testMaxCurrentSessionExceeded() {
        IdentityGlobalConfig.MAX_CONCURRENT_SESSION.updateValue(1)

        createAccount {
            name = "exceed"
            password = "password"
        } as AccountInventory

        logInByAccount {
            accountName = "exceed"
            password = "password"
        }

        LogInByAccountAction action = new LogInByAccountAction()
        action.accountName = "exceed"
        action.password = "password"
        LogInByAccountAction.Result result = action.call()
        assert result.error.details.contains(IdentityErrors.MAX_CONCURRENT_SESSION_EXCEEDED.toString())

        IdentityGlobalConfig.MAX_CONCURRENT_SESSION.resetValue()
    }

    void testValidateSessionApi() {
        def account = createAccount {
            name = "validate"
            password = "password"
        } as AccountInventory

        SessionInventory session = logInByAccount {
            accountName = "validate"
            password = "password"
        } as SessionInventory

        Session.sessions.get(session.uuid).expiredDate = new Timestamp(new Date().getTime() - 1000)

        assert validateSession {
            sessionUuid = session.uuid
        }.valid == true

        deleteAccount {
            uuid = account.uuid
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

        Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()")
        Timestamp now = (Timestamp) query.getSingleResult()
        SessionInventory sess2 = renewSession {
            sessionUuid = sess1.uuid
            duration = 3600L
        }

        assert sess2.uuid == sess1.uuid
        assert sess2.accountUuid == sess1.accountUuid
        assert sess2.userUuid == sess1.userUuid
        /* suppose 30 seconds error */
        assert sess2.expiredDate.getTime() >= now.getTime() + TimeUnit.SECONDS.toMillis(3600)
        assert sess2.expiredDate.getTime() <= now.getTime() + TimeUnit.SECONDS.toMillis(3630L)

        assert acntMgr.getSessionsCopy().get(sess2.uuid).expiredDate.getTime() >= now.getTime() + TimeUnit.SECONDS.toMillis(3600)
        assert acntMgr.getSessionsCopy().get(sess2.uuid).expiredDate.getTime() <= now.getTime() + TimeUnit.SECONDS.toMillis(3630L)
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

    void testInvalidSession() {
        SessionInventory sess1 = logInByAccount {
            accountName = "test1"
            password = "password1"
        } as SessionInventory

        assert acntMgr.getSessionsCopy().get(sess1.uuid) != null

        logOut {
            sessionUuid = sess1.uuid
        }

        expect (AssertionError.class) {
            updateAccount {
                uuid = sess1.accountUuid
                password = "new"
                sessionId = sess1.uuid
            }
        }

        expect (AssertionError.class) {
            queryVmInstance {
                sessionId = sess1.uuid
            }
        }
    }
}
