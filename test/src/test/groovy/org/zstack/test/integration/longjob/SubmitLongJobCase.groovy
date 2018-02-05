package org.zstack.test.integration.longjob

import org.zstack.sdk.SubmitLongJobAction
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by GuoYi on 12/6/17.
 */
class SubmitLongJobCase extends SubCase {
    EnvSpec env

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
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testSubmitLongJobCase()
        }
    }

    void testSubmitLongJobCase() {
        // check jobName
        SubmitLongJobAction action = new SubmitLongJobAction()
        action.sessionId = adminSession()
        action.jobName = "NoSuchApiMessage"
        action.jobData = "{}"
        SubmitLongJobAction.Result res = action.call()
        assert res.error != null
    }
}
