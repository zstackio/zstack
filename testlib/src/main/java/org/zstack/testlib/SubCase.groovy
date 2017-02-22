package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/22.
 */
abstract class SubCase extends Test {
    final void run() {
        try {
            environment()
            test()
        } finally {
            clean()
        }
    }

    @Override
    protected void runSubCases(List<SubCase> cases) {
        throw new Exception("runSubCases() cannot be called in a SubCase")
    }

    abstract void environment()
    abstract void test()
    abstract void clean()
}
