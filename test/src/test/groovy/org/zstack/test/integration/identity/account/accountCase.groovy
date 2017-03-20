package org.zstack.test.integration.identity.account

import org.zstack.header.identity.AccountVO
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.identity.Env
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by AlanJager on 2017/3/23.
 */
class accountCase extends SubCase {
    EnvSpec env
    AccountInventory accountInventory

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
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            accountInventory = createAccount {
                name = "test"
                password = "password"
            }

            testCreateAccountAndResetPasswordOfAdmin()
            testCreateAccountAndResetPassword()
        }
    }

    void testCreateAccountAndResetPasswordOfAdmin() {
        accountInventory = updateAccount {
            uuid = accountInventory.uuid
            password = "new"
        }

        AccountVO accountVO = dbFindByUuid(accountInventory.uuid, AccountVO.class)
        assert accountVO.password  == "new"
    }

    void testCreateAccountAndResetPassword() {
        SessionInventory sessionInventory = logInByAccount {
            accountName = "test"
            password = "new"
        }

        accountInventory = updateAccount {
            uuid = accountInventory.uuid
            password = "password"
            sessionId = sessionInventory.uuid
        }

        AccountVO accountVO = dbFindByUuid(accountInventory.uuid, AccountVO.class)
        assert accountVO.password  == "password"
    }

}
