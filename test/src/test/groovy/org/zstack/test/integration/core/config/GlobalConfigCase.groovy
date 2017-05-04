package org.zstack.test.integration.core.config

import org.zstack.sdk.UpdateGlobalConfigAction
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by miao on 17-5-4.
 */
class GlobalConfigCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testBooleanValidator()
        }
    }

    void testBooleanValidator() {
        updateGlobalConfig {
            category = GlobalConfigForTest.CATEGORY
            name = GlobalConfigForTest.TEST_GlobalConfig2_Boolean.name
            value = "TRUE"
            sessionId = adminSession()
        }


        UpdateGlobalConfigAction action = new UpdateGlobalConfigAction()
        action.category = GlobalConfigForTest.CATEGORY
        action.name = GlobalConfigForTest.TEST_GlobalConfig2_Boolean.name
        action.value = "miao"
        action.sessionId = adminSession()
        UpdateGlobalConfigAction.Result result = action.call()
        assert result.error != null
    }

}
