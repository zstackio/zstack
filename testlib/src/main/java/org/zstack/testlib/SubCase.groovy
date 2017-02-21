package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/22.
 */
abstract class SubCase extends Test {
    final void run() {
        environment()
        test()
    }

    @Override
    final void setup() {
        // do nothing
    }

    abstract void environment()
    abstract void test()
    abstract void clean()
}
