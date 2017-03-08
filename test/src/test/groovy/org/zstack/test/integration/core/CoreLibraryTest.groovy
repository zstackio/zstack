package org.zstack.test.integration.core

import org.zstack.test.integration.core.gc.EventBasedGarbageCollectorCase
import org.zstack.test.integration.core.gc.TimeBasedGarbageCollectorCase
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/3/2.
 */
class CoreLibraryTest extends Test {
    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        runSubCases()
    }
}
