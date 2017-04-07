package org.zstack.test.integration.core.job

import org.zstack.core.Platform
import org.zstack.core.job.JobContext
import org.zstack.core.job.JobQueueFacade
import org.zstack.header.core.Completion
import org.zstack.header.core.NopeCompletion
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by Administrator on 2017-03-24.
 */
//base on TestJob
class FakeJobCase extends SubCase{
    @JobContext
    FakeJobConfig conf
    JobQueueFacade jobf
    long num1 = 50
    long num2 = 10
    int num3 = 100
    long num4 = 10

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        jobf = bean(JobQueueFacade.class)
        TestJob()
        TestJob2()
        TestJobReturnValue()
        TestJobReturnValueFail()
    }
    @Override
    void clean() {

    }
    private void startJob(FakeJob job){
        startJob(job, new NopeCompletion())
    }

    private void startJob(FakeJob job, Completion completion){
        startJob(job, new ReturnValueCompletion<Object>(completion) {
            @Override
            void success(Object returnValue) {
                completion.success()
            }

            @Override
            void fail(ErrorCode errorCode) {
                completion.fail(errorCode)
            }
        },null)
    }

    private <T> void startJob(FakeJob job,  ReturnValueCompletion<T> complete, Class<? extends T> returnType){
        jobf.execute("fake-job", "TestJob", job, complete, returnType)
    }


    void TestJob(){
        conf = new FakeJobConfig()
        for (long i = 0; i < num1; i++) {
            startJob(new FakeJob(){
                @Override
                void run(ReturnValueCompletion<Object> completion) {
                    try {
                        logger.debug(String.format("job %s is executing", i))
                        conf.indexes.add(i)
                    } finally {
                        completion.success(null)
                    }
                }
            })
        }

        TimeUnit.SECONDS.sleep(5)
        long count = 0
        logger.debug(String.format("TestJob after: %d", conf.indexes.size))
        for (Long l : conf.indexes) {
            if (l < count) {
                assert false
            }
            logger.debug(String.format("only for test: %s", l))
            count = l
        }
    }

    void TestJob2(){
        conf = new FakeJobConfig()
        conf.success = true
        for (long i = 0; i < num2; i++) {
            startJob(new FakeJob() {
                @Override
                void run(ReturnValueCompletion<Object> completion) {
                    try {
                        conf.flag = new Random().nextInt()
                        int v = conf.flag
                        try {
                            TimeUnit.MILLISECONDS.sleep(500)
                        } catch (InterruptedException e) {
                            logger.warn(e.getMessage(), e)
                        }
                        if (v != conf.flag) {
                            conf.success = false
                        }
                    } finally {
                        completion.success(null)
                    }
                }
            })
        }

        TimeUnit.SECONDS.sleep(15)
        assert conf.success
    }

    void TestJobReturnValue(){
        boolean success = true
        int retGot = 0
        conf = new FakeJobConfig()
        CountDownLatch latch = new CountDownLatch(num3)

        for (long i = 0; i < num3; i++) {
            startJob(new FakeJob() {
                @Override
                void run(ReturnValueCompletion<Object> completion) {
                    try {
                        logger.debug(String.format("job %s is executing", i))
                    } finally {
                        completion.success(i)
                    }
                }
            }, new ReturnValueCompletion<Long>(null) {
                @Override
                void success(Long returnValue) {
                    logger.debug(String.format("get return value[%s]", returnValue))
                    retGot++
                    if (returnValue != i) {
                        logger.debug(String.format("expect %s but %s", i, returnValue))
                        success = false
                    }
                    latch.countDown()
                }

                @Override
                void fail(ErrorCode errorCode) {
                    logger.debug(errorCode.toString())
                    success = false
                    latch.countDown()
                }
            }, Long.class)
        }

    }

    void TestJobReturnValueFail(){
        boolean success = true
        int retGot = 0

        conf = new FakeJobConfig()

        for (long i = 0; i < num4; i++) {
            startJob(new FakeJob() {
                @Override
                void run(ReturnValueCompletion<Object> completion) {
                    logger.debug(String.format("job %s is executing", i))
                    completion.fail(Platform.operr("fail on purpose"))
                }
            }, new ReturnValueCompletion<Long>(null) {
                @Override
                void success(Long returnValue) {
                    logger.debug(String.format("job[%s] unwanted success", returnValue))
                    success = false
                }

                @Override
                void fail(ErrorCode errorCode) {
                    logger.debug(errorCode.toString())
                    retGot++
                }
            }, Long.class)
        }
    }

}
