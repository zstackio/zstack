package org.zstack.testlib

import org.zstack.sdk.SessionInventory

class SessionSpec extends Spec {
    SessionInventory session

    @SpecParam
    String name
    @SpecParam
    String password

    SessionSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        return null
    }

    String getSessionUuid() {
        session = logInByAccount {
            accountName = name
            delegate.password = password
        }

        return session.uuid
    }

    @Override
    void delete(String sessionId) {
        if (session != null) {
            logOut { sessionUuid = session.uuid }
        }
    }
}
