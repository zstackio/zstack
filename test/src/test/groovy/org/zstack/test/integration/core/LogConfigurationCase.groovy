package org.zstack.test.integration.core

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.AsyncAppender
import org.apache.logging.log4j.core.appender.RollingFileAppender
import org.apache.logging.log4j.core.config.ConfigurationFactory
import org.apache.logging.log4j.core.filter.AbstractFilter
import org.apache.logging.log4j.status.StatusData
import org.apache.logging.log4j.status.StatusListener
import org.apache.logging.log4j.status.StatusLogger
import org.zstack.core.log.Log4jXMLModifier
import org.zstack.core.log.LogConstant
import org.zstack.core.log.LogGlobalConfig
import org.zstack.core.log.LogGlobalProperty
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.logging.LogDropCounterAppender

import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LogConfigurationCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
        }
    }

    @Override
    void test() {
        env.create {
            testUpdateLogConfig()
            testDiagnosticQueueDoesNotBlockProducers()
            testImportantQueueDoesNotBlockProducers()
            testLoggingRecoversAfterSinkFailureAndReload()
            testAutomaticLogConfigurationReload()
        }
    }

    void testDiagnosticQueueDoesNotBlockProducers() {
        testSaturatedQueue(Level.TRACE, "Async", "DiagnosticLogDrops")
    }

    void testImportantQueueDoesNotBlockProducers() {
        testSaturatedQueue(Level.WARN, "AsyncImportant", "ImportantLogDrops")
        testSaturatedQueue(Level.ERROR, "AsyncImportant", "ImportantLogDrops")
    }

    private void withProductionLogConfiguration(Closure test) {
        File directory = Files.createTempDirectory("diagnostic-log-queue-").toFile()
        new File(directory, "logs").mkdirs()
        File configuration = new File(directory, "log4j2.xml")
        configuration.text = new File("../conf/log4j2.xml").getText("UTF-8")
                .replace('${sys:catalina.home}', directory.absolutePath)
                .replace('monitorInterval="30"', 'monitorInterval="0"')
                .replace('450 MB', '64 KB').replace('150 MB', '64 KB')
                .replace('max="50"', 'max="4"')
        LoggerContext context = new LoggerContext("diagnostic-queue-${UUID.randomUUID()}")
        try {
            context.start(ConfigurationFactory.getInstance().getConfiguration(context,
                    context.name, configuration.toURI()))
            test(context, directory, configuration)
        } finally {
            def counters = context.configuration.appenders.values().findAll { it instanceof LogDropCounterAppender }
            context.stop(10, TimeUnit.SECONDS)
            counters.each {
                assert !ManagementFactory.platformMBeanServer.isRegistered(it.objectName) :
                        "log drop MBean must be removed when its configuration stops: ${it.objectName}"
            }
            directory.deleteDir()
        }
    }

    private void testSaturatedQueue(Level level, String appenderName, String counterName) {
        withProductionLogConfiguration { LoggerContext context, File directory, File configuration ->
            assertSaturatedQueue(context, directory, level, appenderName, counterName)
        }
    }

    private void assertSaturatedQueue(LoggerContext context, File directory, Level level,
                                      String appenderName, String counterName) {
        CountDownLatch consumerEntered = new CountDownLatch(1)
        CountDownLatch releaseConsumer = new CountDownLatch(1)
        CountDownLatch releaseStatusOutput = new CountDownLatch(1)
        CountDownLatch producersReturned = new CountDownLatch(64)
        AtomicInteger statusReports = new AtomicInteger()
        StatusListener statusListener = new StatusListener() {
            @Override
            void log(StatusData data) {
                if (data.message.formattedMessage.contains("queue is full")) {
                    statusReports.incrementAndGet()
                    releaseStatusOutput.await(15, TimeUnit.SECONDS)
                }
            }

            @Override
            Level getStatusLevel() {
                return Level.ERROR
            }

            @Override
            void close() {
            }
        }
        def logger = context.getLogger("org.zstack.core.cloudbus.CloudBusImpl3")
        AsyncAppender async = context.configuration.getAppender(appenderName)
        RollingFileAppender sink = context.configuration.getAppender("RollingFile")
        LogDropCounterAppender counter = context.configuration.getAppender(counterName)
        long oldDropped = counter.droppedCount
        long oldImportantDropped = counter.importantDroppedCount
        long oldDiagnosticDrops = context.configuration.getAppender("DiagnosticLogDrops").droppedCount
        long oldImportantDrops = context.configuration.getAppender("ImportantLogDrops").droppedCount
        String pauseMarker = "pause log consumer " + UUID.randomUUID()
        AbstractFilter pausedSink = new AbstractFilter() {
            @Override
            Filter.Result filter(LogEvent event) {
                if (event.message.formattedMessage == pauseMarker) {
                    consumerEntered.countDown()
                    releaseConsumer.await(15, TimeUnit.SECONDS)
                }
                return Filter.Result.NEUTRAL
            }
        }
        def errors = new ConcurrentLinkedQueue<Throwable>()
        def producers = Executors.newFixedThreadPool(64)
        StatusLogger.logger.registerListener(statusListener)
        try {
            assert logger.traceEnabled : "production CloudBus TRACE must be enabled for the queue regression"
            retryInSecs(5) {
                assert async.queueSize == 0 : "previous log updates must drain: queueDepth=${async.queueSize}"
            }
            sink.addFilter(pausedSink)
            logger.log(level, pauseMarker)
            assert consumerEntered.await(5, TimeUnit.SECONDS) : "actual Async consumer did not reach the paused sink"
            64.times {
                producers.execute {
                    try {
                        100.times { logger.log(level, "log queue saturation ${it}") }
                    } catch (Throwable error) {
                        errors.add(error)
                    } finally {
                        producersReturned.countDown()
                    }
                }
            }
            boolean returned = producersReturned.await(3, TimeUnit.SECONDS)
            assert returned : "${level} log producers must return while the consumer and status output are paused: " +
                    "remaining=${producersReturned.count}, queueDepth=${async.queueSize}, capacity=${async.queueCapacity}"
            assert errors.empty : "queue saturation must not fail business callers: errors=${errors}"
            assert statusReports.get() == 0 :
                    "queue overflow must not invoke synchronous StatusLogger on producer threads: actual=${statusReports.get()}"
            assert counter.droppedCount > oldDropped :
                    "${level} full queue must count new dropped messages: before=${oldDropped}, actual=${counter.droppedCount}"
            assert ManagementFactory.platformMBeanServer.getAttribute(counter.objectName, "DroppedCount") == counter.droppedCount :
                    "${level} discarded messages must be visible through JMX"
            assert counter.lastDropTimestamp > 0 : "${level} drop time must be recorded: actual=${counter.lastDropTimestamp}"
            if (level == Level.TRACE) {
                String importantMarker = "important message while diagnostic queue is full " + UUID.randomUUID()
                logger.warn(importantMarker)
                retryInSecs(5) {
                    assert new File(directory, "logs/management-server.log").text.contains(importantMarker) :
                            "WARN must use its reserved queue while diagnostic consumption is paused"
                }
                assert counter.importantDroppedCount == oldImportantDropped : "diagnostic queue must not receive WARN/ERROR messages"
                assert context.configuration.getAppender("ImportantLogDrops").droppedCount == oldImportantDrops :
                        "diagnostic saturation must not consume important queue capacity"
            } else {
                assert counter.importantDroppedCount - oldImportantDropped == counter.droppedCount - oldDropped :
                        "${level} overflow must separately count important log loss"
                assert context.configuration.getAppender("DiagnosticLogDrops").droppedCount == oldDiagnosticDrops :
                        "important messages must not enter the diagnostic queue"
            }
        } finally {
            releaseConsumer.countDown()
            releaseStatusOutput.countDown()
            producers.shutdownNow()
            try {
                assert producers.awaitTermination(10, TimeUnit.SECONDS) : "log producer threads did not terminate"
            } finally {
                StatusLogger.logger.removeListener(statusListener)
                sink.removeFilter(pausedSink)
            }
        }
    }

    void testLoggingRecoversAfterSinkFailureAndReload() {
        withProductionLogConfiguration { LoggerContext context, File directory, File configuration ->
            def logger = context.getLogger("org.zstack.core.cloudbus.CloudBusImpl3")
            RollingFileAppender sink = context.configuration.getAppender("RollingFile")
            LogDropCounterAppender counter = context.configuration.getAppender("ImportantLogDrops")
            sink.addFilter(new AbstractFilter() {
                @Override
                Filter.Result filter(LogEvent event) {
                    if (event.message.formattedMessage == "injected log sink failure") {
                        throw new IllegalStateException("injected log sink failure")
                    }
                    return Filter.Result.NEUTRAL
                }
            })
            logger.error("injected log sink failure")
            retryInSecs(5) {
                assert counter.importantDroppedCount == 1 :
                        "failed sink delivery must be counted: actual=${counter.importantDroppedCount}"
            }
            logger.error("sink recovered")
            retryInSecs(5) {
                assert new File(directory, "logs/management-server.log").text.contains("sink recovered") :
                        "important logging must continue after a consumer exception"
            }
            String payload = "rolling regression " + "x" * 2048
            2000.times { logger.trace(payload) }
            retryInSecs(10) {
                assert new File(directory, "logs").listFiles().any { it.name.endsWith(".gz") } :
                        "production RollingFile must roll and compress during saturation"
            }
            def oldCounterNames = context.configuration.appenders.values()
                    .findAll { it instanceof LogDropCounterAppender }.collect { it.objectName }
            context.setConfigLocation(configuration.toURI())
            oldCounterNames.each {
                assert !ManagementFactory.platformMBeanServer.isRegistered(it) :
                        "configuration reload must remove old drop counters: ${it}"
            }
            ["Async", "AsyncImportant"].each {
                assert !((AsyncAppender) context.configuration.getAppender(it)).blocking :
                        "configuration reload must retain the nonblocking policy: ${it}"
            }
            logger.warn("important logging after reload")
            retryInSecs(5) {
                assert new File(directory, "logs/management-server.log").text.contains("important logging after reload") :
                        "important logging must remain available after configuration reload"
            }
            assertSaturatedQueue(context, directory, Level.TRACE, "Async", "DiagnosticLogDrops")
            assertSaturatedQueue(context, directory, Level.WARN, "AsyncImportant", "ImportantLogDrops")
        }
    }

    void testAutomaticLogConfigurationReload() {
        withProductionLogConfiguration { LoggerContext context, File directory, File configuration ->
            configuration.text = configuration.text.replace('monitorInterval="0"', 'monitorInterval="1"')
            context.setConfigLocation(configuration.toURI())
            def oldCounterNames = context.configuration.appenders.values()
                    .findAll { it instanceof LogDropCounterAppender }.collect { it.objectName }
            configuration.text = configuration.text.replace('bufferSize="512"', 'bufferSize="513"')
            assert configuration.setLastModified(System.currentTimeMillis() + 2000) :
                    "test configuration timestamp must change for automatic monitoring"
            retryInSecs(15) {
                ["Async", "AsyncImportant"].each {
                    AsyncAppender async = context.configuration.getAppender(it)
                    assert async.queueCapacity == 513 && !async.blocking :
                            "monitorInterval must reload ${it}: capacity=${async.queueCapacity}, blocking=${async.blocking}"
                }
                oldCounterNames.each {
                    assert !ManagementFactory.platformMBeanServer.isRegistered(it) :
                            "automatic reload must remove old drop counters: ${it}"
                }
            }
            context.getLogger("org.zstack.core.cloudbus.CloudBusImpl3").error("important logging after automatic reload")
            retryInSecs(5) {
                assert new File(directory, "logs/management-server.log").text.contains("important logging after automatic reload") :
                        "important logging must work after monitorInterval reload"
            }
            assertSaturatedQueue(context, directory, Level.TRACE, "Async", "DiagnosticLogDrops")
            assertSaturatedQueue(context, directory, Level.WARN, "AsyncImportant", "ImportantLogDrops")
        }
    }

    void testUpdateLogConfig() {
        long errorSize = Integer.MAX_VALUE + 1
        expect(AssertionError.class){
            updateGlobalConfig {
                category = LogGlobalConfig.CATEGORY
                name = LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.getName()
                value = errorSize
            }
        }

        LogGlobalProperty.LOG_RETENTION_SIZE_GB = -2
        updateGlobalConfig {
            category = LogGlobalConfig.CATEGORY
            name = LogGlobalConfig.LOG_DELETE_LAST_MODIFIED.getName()
            value = "2"
        }
        Log4jXMLModifier log4jXMLModifier = new Log4jXMLModifier()
        assert "2d" == log4jXMLModifier.getLogRetentionDays(LogConstant.MN_SERVER_LOG_ROLLINGFILE)
        assert "2d" == log4jXMLModifier.getLogRetentionDays(LogConstant.MN_API_LOG_ROLLINGFILE)

        updateGlobalConfig {
            category = LogGlobalConfig.CATEGORY
            name = LogGlobalConfig.LOG_DELETE_LAST_MODIFIED.getName()
            value = "-1"
        }
        log4jXMLModifier = new Log4jXMLModifier()
        assert "-1d" == log4jXMLModifier.getLogRetentionDays(LogConstant.MN_SERVER_LOG_ROLLINGFILE)
        assert "-1d" == log4jXMLModifier.getLogRetentionDays(LogConstant.MN_API_LOG_ROLLINGFILE)

        updateGlobalConfig {
            category = LogGlobalConfig.CATEGORY
            name = LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.getName()
            value = "10"
        }
        log4jXMLModifier = new Log4jXMLModifier()
        assert "8192 MB" == log4jXMLModifier.getLogRetentionSize(LogConstant.MN_SERVER_LOG_ROLLINGFILE)
        assert "2048 MB" == log4jXMLModifier.getLogRetentionSize(LogConstant.MN_API_LOG_ROLLINGFILE)

        updateGlobalConfig {
            category = LogGlobalConfig.CATEGORY
            name = LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.getName()
            value = "-1"
        }
        log4jXMLModifier = new Log4jXMLModifier()
        assert "-1 MB" == log4jXMLModifier.getLogRetentionSize(LogConstant.MN_SERVER_LOG_ROLLINGFILE)
        assert "-1 MB" == log4jXMLModifier.getLogRetentionSize(LogConstant.MN_API_LOG_ROLLINGFILE)

        LogGlobalProperty.LOG_RETENTION_SIZE_GB = 200
        expect(AssertionError.class){
            updateGlobalConfig {
                category = LogGlobalConfig.CATEGORY
                name = LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.getName()
                value = "300"
            }
        }
    }
}
