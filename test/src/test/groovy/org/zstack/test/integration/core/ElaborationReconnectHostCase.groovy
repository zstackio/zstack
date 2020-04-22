package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class ElaborationReconnectHostCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {
        env = new EnvSpec()
    }

    @Override
    void test() {
        testElaboration()
    }


    void testElaboration() {
        def err = Platform.operr("Unable to reconnect host") as ErrorCode
        assert err.elaboration != null
        assert err.elaboration.trim() == "错误信息: 无法重连物理机"

    }

}

