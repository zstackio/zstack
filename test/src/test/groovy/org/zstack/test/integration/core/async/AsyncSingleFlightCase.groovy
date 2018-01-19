package org.zstack.test.integration.core.async

import org.zstack.core.singleflight.AsyncSingleFlight
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.SubCase

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AsyncSingleFlightCase extends SubCase {
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
        testAsyncSingleFlight()
    }

    class CustomChecker {
        private AtomicInteger cnt = new AtomicInteger(0)
        private volatile done = false

        boolean fakeCheck(String key) {
            cnt.incrementAndGet()

            while (!done) {
                TimeUnit.MICROSECONDS.sleep(100)
            }

            return true
        }

        int getCounter() {
            return cnt.get()
        }

        void setDone() {
            done = true
        }
    }

    void testAsyncSingleFlight() {
        final int totalCnt = 100
        final String key = "foo"

        AtomicInteger threadCounter = new AtomicInteger(0)
        CustomChecker c = new CustomChecker()
        AsyncSingleFlight<Boolean> sf = new AsyncSingleFlight<>()

        1.upto(totalCnt) {
            Thread.start {
                logger.info("called " + threadCounter.incrementAndGet())

                sf.execute(key, new Callable<Boolean>() {
                    @Override
                    Boolean call() throws Exception {
                        return c.fakeCheck(key)
                    }
                }, new ReturnValueCompletion<Boolean>(null) {
                    @Override
                    void success(Boolean returnValue) {
                        assert returnValue
                    }

                    @Override
                    void fail(ErrorCode errorCode) {
                        assert false
                    }
                })
            }
        }

        while (threadCounter.get() != totalCnt) {
            TimeUnit.MICROSECONDS.sleep(100)
        }

        c.setDone()

        assert c.getCounter() == 1
    }
}
