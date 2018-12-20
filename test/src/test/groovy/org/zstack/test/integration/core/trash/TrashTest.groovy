package org.zstack.test.integration.core.trash


import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.Test
/**
 * Created by mingjian.deng on 2018/12/20.*/
class TrashTest extends Test {
    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }
}
