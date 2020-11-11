package org.zstack.test.integration.core.chaintask

import org.zstack.core.singleflight.CompletionSingleFlight
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CompletionSingleFlightCase extends SubCase {
    private static final String KEY = "foo"
    
    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testCompletionSingleFlight()
    }

    class CustomChecker {
        private AtomicInteger cnt = new AtomicInteger(0)
        private volatile done = false

        boolean fakeCheck(String key) {
            cnt.incrementAndGet()

            while (!done) {
                TimeUnit.MICROSECONDS.sleep(100)
            }

            return key == KEY
        }

        int getCounter() {
            return cnt.get()
        }

        void setDone() {
            done = true
        }
    }

    void testCompletionSingleFlight() {
        final int totalCnt = 100

        AtomicInteger threadCounter = new AtomicInteger(0)
        CustomChecker c = new CustomChecker()
        CompletionSingleFlight<Boolean> sf = new CompletionSingleFlight<>()
        Boolean success = false

        1.upto(totalCnt) {
            Thread.start {
                logger.info("called " + threadCounter.incrementAndGet())

                sf.execute(KEY, { comp ->
                    boolean b = c.fakeCheck(KEY)
                    comp.success(b)
                }, new ReturnValueCompletion<Boolean>(null) {
                    @Override
                    void success(Boolean returnValue) {
                        success = returnValue
                    }

                    @Override
                    void fail(ErrorCode errorCode) {
                        success = false
                    }
                })
            }
        }

        int whileCount = 0
        while (sf.calls.get(KEY) == null || sf.calls.get(KEY).size() != totalCnt || c.getCounter() == 0) {
            TimeUnit.MILLISECONDS.sleep(100)
            assert whileCount ++ < 20
        }

        c.setDone()

        assert !retryInSecs(2){
            return c.getCounter() != 1
        }

        assert success
    }
}
