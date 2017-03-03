package org.zstack.testlib

/**
 * Created by xing5 on 2017/3/3.
 */
trait Case {
    abstract void environment()
    abstract void test()
    abstract void clean()
    abstract void run()
}