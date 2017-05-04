package org.zstack.test.integration.identity.account

import org.zstack.header.identity.AccountVO
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.identity.Env
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.StopTestSuiteException
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/3/23.
 */
class TestFailCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
        throw new StopTestSuiteException()
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

        }
    }
}
