package org.zstack.test.integration.core.taskqueue

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.config.Property
import org.zstack.core.Platform
import org.zstack.core.thread.ThreadFacade
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SyncTaskLoggingIsolationCase extends SubCase {
    ThreadFacade thdf
    BlockingAppender blockingAppender
    LoggerContext loggerContext
    LoggerConfig loggerConfig

    @Override
    void clean() {
        blockingAppender?.release()
        removeBlockingAppender()
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
        thdf = bean(ThreadFacade.class)
    }

    @Override
    void test() {
        testBlockedQueueLoggingDoesNotBlockOtherSignatures()
        testStatisticsCollectionDoesNotLog()
    }

    void testBlockedQueueLoggingDoesNotBlockOtherSignatures() {
        String blockedSignature = Platform.uuid
        String independentSignature = Platform.uuid
        CountDownLatch firstTaskStarted = new CountDownLatch(1)
        CountDownLatch releaseFirstTask = new CountDownLatch(1)
        CountDownLatch independentSubmitReturned = new CountDownLatch(1)

        submitTask(blockedSignature, {
            firstTaskStarted.countDown()
            releaseFirstTask.await()
        })
        Thread blockedSubmitter
        Thread independentSubmitter
        try {
            assert firstTaskStarted.await(5, TimeUnit.SECONDS)
            installBlockingAppender(blockedSignature)

            blockedSubmitter = Thread.start {
                submitTask(blockedSignature, {})
            }
            assert blockingAppender.logEntered.await(5, TimeUnit.SECONDS)

            independentSubmitter = Thread.start {
                submitTask(independentSignature, {})
                independentSubmitReturned.countDown()
            }
            assert independentSubmitReturned.await(3, TimeUnit.SECONDS)
        } finally {
            blockingAppender?.release()
            releaseFirstTask.countDown()
            joinThread(blockedSubmitter)
            joinThread(independentSubmitter)
            removeBlockingAppender()
        }
    }

    void testStatisticsCollectionDoesNotLog() {
        String observedSignature = Platform.uuid
        CountDownLatch observedTaskStarted = new CountDownLatch(1)
        CountDownLatch releaseObservedTask = new CountDownLatch(1)
        CountDownLatch statisticsReturned = new CountDownLatch(1)

        submitTask(observedSignature, {
            observedTaskStarted.countDown()
            releaseObservedTask.await()
        })
        Thread statisticsReader
        try {
            assert observedTaskStarted.await(5, TimeUnit.SECONDS)
            installBlockingAppender("\"syncSignature\":\"${observedSignature}\"")

            statisticsReader = Thread.start {
                thdf.syncTaskStatistics
                statisticsReturned.countDown()
            }
            assert !blockingAppender.logEntered.await(1, TimeUnit.SECONDS)
            assert statisticsReturned.await(3, TimeUnit.SECONDS)
        } finally {
            blockingAppender?.release()
            releaseObservedTask.countDown()
            joinThread(statisticsReader)
            removeBlockingAppender()
        }
    }

    private void installBlockingAppender(String messageFragment) {
        blockingAppender = new BlockingAppender(messageFragment)
        blockingAppender.start()
        loggerContext = (LoggerContext) LogManager.getContext(false)
        loggerConfig = loggerContext.configuration.getLoggerConfig("org.zstack.core.thread.DispatchQueueImpl")
        loggerConfig.addAppender(blockingAppender, Level.DEBUG, null)
        loggerContext.updateLoggers()
    }

    private void removeBlockingAppender() {
        if (loggerConfig != null && blockingAppender != null) {
            loggerConfig.removeAppender(blockingAppender.name)
            blockingAppender.stop()
            loggerContext.updateLoggers()
        }
        blockingAppender = null
        loggerConfig = null
        loggerContext = null
    }

    private void submitTask(String signature, Closure execution) {
        thdf.syncSubmit(new TestSyncTask(signature, signature, 1, execution as Runnable))
    }

    private static void joinThread(Thread thread) {
        if (thread == null) {
            return
        }

        thread.join(5000)
        if (thread.alive) {
            thread.interrupt()
            thread.join(5000)
        }
        assert !thread.alive: "thread ${thread.name} did not stop"
    }

    private static class BlockingAppender extends AbstractAppender {
        final String messageFragment
        final CountDownLatch logEntered = new CountDownLatch(1)
        final CountDownLatch releaseLog = new CountDownLatch(1)

        BlockingAppender(String messageFragment) {
            super("sync-task-blocking-appender-${UUID.randomUUID()}", null, null, false, Property.EMPTY_ARRAY)
            this.messageFragment = messageFragment
        }

        @Override
        void append(LogEvent event) {
            if (!event.message.formattedMessage.contains(messageFragment)) {
                return
            }

            logEntered.countDown()
            releaseLog.await(10, TimeUnit.SECONDS)
        }

        void release() {
            releaseLog.countDown()
        }
    }
}
