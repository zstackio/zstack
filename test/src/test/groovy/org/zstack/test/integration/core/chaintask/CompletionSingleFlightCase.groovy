package org.zstack.test.integration.core.chaintask

import org.apache.logging.log4j.ThreadContext
import org.zstack.core.singleflight.CompletionSingleFlight
import org.zstack.core.singleflight.TaskSingleFlight
import org.zstack.header.Constants
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.core.progress.RunningTaskInfo
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

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
        testSingleFlightHandleException()
        testTaskSingleFlight()
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
        CompletionSingleFlight<String, Boolean> sf = new CompletionSingleFlight<>()
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
        while (sf.calls.get(KEY) == null || sf.calls.get(KEY).count() != totalCnt || c.getCounter() == 0) {
            TimeUnit.MILLISECONDS.sleep(100)
            assert whileCount ++ < 20
        }

        c.setDone()

        assert !retryInSecs(2){
            return c.getCounter() != 1
        }

        assert success
    }

    void testSingleFlightHandleException() {
        CompletionSingleFlight<String, Boolean> singleFlight = new CompletionSingleFlight<>()
        def threadCounter = new AtomicInteger(0)
        final threadCount = 100
        def threadCreateLatch = new CountDownLatch(threadCount - 1) // 1 execute and others wait
        def resultLatch = new CountDownLatch(threadCount)
        final values = new int[threadCount + 1] // values[0] is invalid, range from 1 to 100, length = 101

        1.upto(threadCount) {
            Thread.start {
                final self = threadCounter.incrementAndGet()
                logger.info("called " + self)
                singleFlight.execute(KEY, { comp ->
                    threadCreateLatch.await(5, TimeUnit.SECONDS)
                    throw new RuntimeException("on purpose")
                }, new ReturnValueCompletion<Boolean>(null) {
                    @Override
                    void success(Boolean returnValue) {
                        values[self] = returnValue ? 1 : 0
                        resultLatch.countDown()
                    }

                    @Override
                    void fail(ErrorCode errorCode) {
                        values[self] = -1
                        resultLatch.countDown()
                    }
                })
                threadCreateLatch.countDown()
            }
        }

        assert resultLatch.await(10, TimeUnit.SECONDS) // return false -> timeout
        for (int i = 1; i < values.length; i++) {
            assert values[i] == -1
        }
    }

    /**
     * main thread -> process task
     * sub thread -> check running task info from context
     */
    void testTaskSingleFlight() {
        TaskSingleFlight<String, Boolean> singleFlight = new TaskSingleFlight<>()
        def mainThreadLock = new ReentrantLock()
        def subThreadLock = new ReentrantLock()
        subThreadLock.lock()
        //     lock 1 = subThreadLock   lock 2 = mainThreadLock
        // main thread : lock 1
        // sub thread  : lock 2
        // sub thread  : wait for lock 1
        // main thread : start single flight execution
        // main thread : unlock 1
        // main thread : wait for lock 2
        // sub thread  : get running task info
        // sub thread  : unlock 2
        // main thread : finish single flight execution

        // sub thread
        RunningTaskInfo taskInfo = null
        Thread.start {
            mainThreadLock.lock()
            if (!subThreadLock.tryLock(3, TimeUnit.SECONDS)) {
                logger.error("get task info fail because time out")
                return // wait for time out
            }
            taskInfo = singleFlight.buildRunningTaskInfo(KEY)
            logger.info("running task info: ${taskInfo.toString()}")
            mainThreadLock.unlock()
        }
        
        singleFlight.execute(KEY, { comp ->
            // in main thread
            sleep 1000
            subThreadLock.unlock()
            assert mainThreadLock.tryLock(5, TimeUnit.SECONDS)
            comp.success(true)
        }, new ReturnValueCompletion<Boolean>(null) {
            @Override
            void success(Boolean returnValue) {
                assert returnValue
            }

            @Override
            void fail(ErrorCode errorCode) {
                logger.warn("should not be here")
                assert false
            }
        })

        assert taskInfo != null
        assert taskInfo.name == KEY
    }
}
