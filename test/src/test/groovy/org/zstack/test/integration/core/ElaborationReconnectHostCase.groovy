package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.HostErrors
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
        assert err.elaboration.trim() == "错误信息: 无法重连主机。"

        def err1 = Platform.operr("failed to create bridge") as ErrorCode
        assert err1.elaboration == null

        def err2 = Platform.err(HostErrors.CONNECTION_ERROR, err1, "connection error for KVM host[uuid:%s, ip:%s]", Platform.getUuid(), "127.0.0.1") as ErrorCode
        assert err2.getElaboration() == null

        def err3 =  Platform.err(HostErrors.CONNECTION_ERROR, err2, "connection error for KVM host[uuid:%s, ip:%s]", Platform.getUuid(), "127.0.0.1") as ErrorCode
        assert err3.getElaboration() == null

        def err4 = Platform.err(SysErrors.OPERATION_ERROR, err, "failed to create bridge") as ErrorCode
        assert err4.getElaboration() != null
        assert err4.elaboration.trim() == "错误信息: 无法重连主机。"
    }

}

