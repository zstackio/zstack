package org.zstack.test.integration.core

import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/3/2.
 */
class CoreLibraryTest extends Test {
    @Override
    void setup() {
        spring {
            include("CloudBusAopProxy.xml")
            include("JobForUnitTest.xml")
        }
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        runSubCases()
    }
}
