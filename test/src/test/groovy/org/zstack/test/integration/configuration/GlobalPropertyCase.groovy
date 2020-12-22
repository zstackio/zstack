package org.zstack.test.integration.configuration

import org.zstack.core.CoreGlobalProperty
import org.zstack.sdk.GetGlobalPropertyAction
import org.zstack.sdk.MigrateVmAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

/**
 * Created by GuoYi on 12/21/20.
 */
class GlobalPropertyCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    static SpringSpec springSpec = makeSpring {
        include("ConfigurationManager.xml")
    }

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
        env = env {}
    }

    @Override
    void test() {
        env.create {
            testGetGlobalProperties()
        }
    }

    void testGetGlobalProperties() {
        GetGlobalPropertyAction action = new GetGlobalPropertyAction()
        action.sessionId = adminSession()
        GetGlobalPropertyAction.Result result = action.call()
        assert "unitTestOn: true" in result.value.properties
    }
}
