package org.zstack.test.integration.core.async

import org.zstack.core.thread.SyncTaskChain
import org.zstack.header.console.APIReconnectConsoleProxyAgentMsg
import org.zstack.header.core.Completion
import org.zstack.header.core.FutureReturnValueCompletion
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

class FutureCompletionCase extends SubCase{
    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testFutureCompletion()
    }

    void testFutureCompletion(){
        def msg = new APIReconnectConsoleProxyAgentMsg()
        FutureReturnValueCompletion completion = new FutureReturnValueCompletion(msg);
//        completion.SLOW_FUTURE_TIMEOUT = 100
        Thread thread1 = Thread.start {
            TimeUnit.MILLISECONDS.sleep(500)
            completion.success()
        }
        completion.await(TimeUnit.SECONDS.toMillis(1))
        thread1.join()

        completion = new FutureReturnValueCompletion(null);
//        completion.SLOW_FUTURE_TIMEOUT = 100
        thread1 = Thread.start {
            TimeUnit.MILLISECONDS.sleep(500)
            completion.success()
        }
        completion.await(TimeUnit.SECONDS.toMillis(1))
        thread1.join()

        completion = new FutureReturnValueCompletion(null);
//        completion.SLOW_FUTURE_TIMEOUT = 1000
        thread1 = Thread.start {
            TimeUnit.MILLISECONDS.sleep(3000)
            completion.success()
        }

        logger.debug("start completion wait")
        completion.await(TimeUnit.SECONDS.toMillis(2));
        logger.debug("completion wait finished, it should last 2 seconds to start")
        thread1.join()

        def acompletion = new Completion(null) {
            @Override
            void success() {

            }

            @Override
            void fail(ErrorCode errorCode) {

            }
        }
        completion = new FutureReturnValueCompletion(acompletion);
//        completion.SLOW_FUTURE_TIMEOUT = 100
        thread1 = Thread.start {
            TimeUnit.MILLISECONDS.sleep(2000)
            completion.success()
        }
        completion.await(TimeUnit.SECONDS.toMillis(1));
        logger.debug(String.format("%s", completion.success))
        logger.debug(String.format("%s", completion.getResult()))
        thread1.join()
    }

    @Override
    void clean() {

    }
}