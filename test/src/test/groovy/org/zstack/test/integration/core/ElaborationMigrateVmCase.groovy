package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.HostErrors
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class ElaborationMigrateVmCase extends SubCase {
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
        def err = Platform.operr("failed to migrate vm[uuid:54a8af3843094c53a8fd2b87bbbf95c4] from kvm host[uuid:f3712c38be0742f6b9c815b305685e98, ip:172.24.197.225] to dest host[ip:172.24.193.169], No enough physical memory for guest") as ErrorCode
        assert err.elaboration != null
        assert err.elaboration.trim() == "错误信息: 在物理机上迁移云主机失败，因为物理机已经没有足够的物理内存可供云主机使用。"
    }

}

