package org.zstack.test.integration.other.mysqlschema

import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/2/27.
 */
class MustPassCase extends SubCase {

    @Override
    void clean() {
    }

    @Override
    void setup() {
        useSpring(Mysql57Test.springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        assert true
    }
}
