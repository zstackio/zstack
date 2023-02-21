package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCodeList
import org.zstack.header.storage.primary.PrimaryStorageErrors
import org.zstack.header.vm.VmErrors
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.string.ElaborationSearchMethod

import static org.zstack.core.Platform.err

/**
 * Created by mingjian.deng on 2019/7/13.*/
class DistanceElaborationCase extends SubCase {
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
        testElaboration1()
    }

    void testElaboration1() {
        def err = Platform.operr("arg 'startTime' should format like 'yyyy-MM-dd HH:mm:ss' or '1545380003000'") as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.distance
        assert err.messages.message_cn == "输入参数中 'startTime' 的格式应该类似 'yyyy-MM-dd HH:mm:ss' 或 '1545380003000'。"
    }
}
