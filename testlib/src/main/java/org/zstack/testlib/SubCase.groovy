package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/22.
 */
abstract class SubCase extends Test implements Case {
    final void run() {
        try {
            environment()
            test()
        } catch (Throwable t) {
            logger.warn("a sub case [${this.class}] fails, ${t.message}", t)
            collectErrorLog()
            throw t
        } finally {
            logger.info("start cleanup for case ${this.class}")
            try{
                clean()
            }catch (Throwable t){
                collectErrorLog()
                throw t
            }
        }
    }

    @Override
    protected void runSubCases() {
        throw new Exception("runSubCases() cannot be called in a SubCase")
    }
}
