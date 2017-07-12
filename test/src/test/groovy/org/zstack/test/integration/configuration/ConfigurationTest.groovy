package org.zstack.test.integration.configuration

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/27.
 */
class ConfigurationTest extends Test {
    static SpringSpec springSpec = makeSpring {
        // no need to include anything, just use core services
    }

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        runSubCases()
    }

}
