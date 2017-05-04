package org.zstack.test.integration.core.config

import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.Test

/**
 * Created by miao on 17-5-5.
 */
class GlobalConfigTest extends Test {
    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        runSubCases()
    }
}
