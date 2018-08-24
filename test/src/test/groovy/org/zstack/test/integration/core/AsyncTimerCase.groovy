package org.zstack.test.integration.core

import org.zstack.core.thread.AsyncTimer
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

class AsyncTimerCase extends SubCase {
    @Override
    void clean() {

    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    void testRun() {
        int count = 0
        int total = 3

        def timer = new AsyncTimer(TimeUnit.SECONDS, 1L) {
            @Override
            protected void execute() {
                count ++
                if (count < total) {
                    continueToRunThisTimer()
                }
            }
        }

        timer.start()

        TimeUnit.SECONDS.sleep(total)
        retryInSecs {
            assert total == count
        }

        TimeUnit.SECONDS.sleep(2L)

        assert total == count
    }

    void testCancel() {
        int count = 0

        def timer = new AsyncTimer(TimeUnit.SECONDS, 1L) {
            @Override
            protected void execute() {
                count ++
                continueToRunThisTimer()
            }
        }

        timer.start()
        TimeUnit.SECONDS.sleep(2L)
        timer.cancel()
        int now = count
        TimeUnit.SECONDS.sleep(2L)
        assert now == count
    }

    @Override
    void test() {
        testRun()
        testCancel()
    }
}
