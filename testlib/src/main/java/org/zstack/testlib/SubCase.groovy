package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/22.
 */
abstract class SubCase extends Test implements Case {
    final void run() {
        try {
            environment()
            test()
        } finally {
            clean()
        }
    }

    @Override
    protected void runSubCases(List<Case> cases) {
        throw new Exception("runSubCases() cannot be called in a SubCase")
    }
}
