package org.zstack.test.integration.core

import org.zstack.core.asyncbatch.AsyncForEach
import org.zstack.core.asyncbatch.AsyncForEachCompletion
import org.zstack.core.asyncbatch.BatchResult
import org.zstack.core.asyncbatch.BatchTermination
import org.zstack.core.asyncbatch.FailurePolicy
import org.zstack.core.asyncbatch.ItemStatus
import org.zstack.header.core.FutureCompletion
import org.zstack.test.TestSafeAsyncForEach
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.util.Collections
import java.util.List
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration scenarios for AsyncForEach, kept alongside WhileCase so that
 * the two batch-loop implementations are exercised with comparable cases.
 */
class AsyncForEachCase extends SubCase {
    private static final CLogger logger = Utils.getLogger(AsyncForEachCase.class)
    static final long TIME_OUT = TimeUnit.SECONDS.toMillis(15)

    @Override
    void clean() {
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testExceptionHandle()
        testStepSmallerThanItems()
        testRunAllWhenItemsEmpty()
        testRunAllCompletionBreak()
        testRunStepWhenItemsEmpty()
        testRunStepCompletionDone()
        testRunStepCompletionBreak()
        testConcurrentAdd()
        testRunAll()
        testRunAllWithConcurrencyZero()
    }

    static void testExceptionHandle() {
        new TestSafeAsyncForEach().testSafeAsyncForEach()
    }

    static void testStepSmallerThanItems() {
        AtomicInteger count = new AtomicInteger()
        BatchResult result = runLoop(AsyncForEach.of([1, 2, 3])
                .concurrency(2)
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each(asBody { item, completion ->
                    count.incrementAndGet()
                    completion.success(null)
                }))

        assert count.get() == 3
        assert result.successCount() == 3
    }

    static void testRunAllWhenItemsEmpty() {
        BatchResult result = runLoop(AsyncForEach.of([])
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each(asBody { item, completion ->
                    assert false: "body must not be called"
                }))

        assert result.getTermination() == BatchTermination.ALL_ITEMS_PROCESSED
        assert result.totalCount() == 0
    }

    static void testRunAllCompletionBreak() {
        BatchResult result = runLoop(AsyncForEach.of(["1", "2"])
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each(asBody { iteration, completion ->
                    completion.breakLoop("found", "target found")
                }))

        assert result.getTermination() == BatchTermination.STOPPED_BY_ITEM
        assert result.getStopReason() == "target found"
        assert result.getItemResults()[0].getResult() == "found"
        assert result.getItemResults()[1].getStatus() == ItemStatus.NOT_STARTED
    }

    static void testRunStepWhenItemsEmpty() {
        BatchResult result = runLoop(AsyncForEach.of(Collections.emptyList())
                .concurrency(1)
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each(asBody { item, completion ->
                    assert false: "body must not be called"
                }))

        assert result.getTermination() == BatchTermination.ALL_ITEMS_PROCESSED
        assert result.totalCount() == 0
    }

    static void testRunStepCompletionDone() {
        AtomicInteger count = new AtomicInteger()
        BatchResult result = runLoop(AsyncForEach.of([1, 2, 3])
                .concurrency(2)
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each(asBody { iteration, completion ->
                    Thread.start {
                        TimeUnit.MILLISECONDS.sleep(iteration.item() * 100L)
                        count.incrementAndGet()
                        completion.success(null)
                    }
                }))

        assert result.getTermination() == BatchTermination.ALL_ITEMS_PROCESSED
        assert count.get() == 3
        assert result.successCount() == 3
    }

    static void testRunStepCompletionBreak() {
        CountDownLatch secondItemStarted = new CountDownLatch(1)
        CountDownLatch breakSent = new CountDownLatch(1)
        CountDownLatch allowSecondItemToComplete = new CountDownLatch(1)
        CountDownLatch batchDone = new CountDownLatch(1)
        AtomicReference<BatchResult> resultRef = new AtomicReference<>()

        AsyncForEach.of([1, 2, 3, 4])
                .concurrency(2)
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each(asBody { iteration, completion ->
                    if (iteration.index() == 0) {
                        Thread.start {
                            await(secondItemStarted)
                            completion.breakLoop("winner", "result found")
                            breakSent.countDown()
                        }
                    } else {
                        Thread.start {
                            secondItemStarted.countDown()
                            await(allowSecondItemToComplete)
                            completion.success("in-flight")
                        }
                    }
                })
                .execute(new AsyncForEachCompletion(null) {
                    @Override
                    void completed(BatchResult result) {
                        resultRef.set(result)
                        batchDone.countDown()
                    }
                })

        assert breakSent.await(TIME_OUT, TimeUnit.MILLISECONDS)
        assert batchDone.getCount() == 1

        allowSecondItemToComplete.countDown()
        assert batchDone.await(TIME_OUT, TimeUnit.MILLISECONDS)
        BatchResult result = resultRef.get()
        assert result.getTermination() == BatchTermination.STOPPED_BY_ITEM
        assert result.getStopReason() == "result found"
        assert result.getItemResults()[0].getResult() == "winner"
        assert result.getItemResults()[1].getStatus() == ItemStatus.SUCCEEDED
        assert result.getItemResults()[2].getStatus() == ItemStatus.NOT_STARTED
        assert result.getItemResults()[3].getStatus() == ItemStatus.NOT_STARTED
    }

    static void testConcurrentAdd() {
        List source = (0..<100).collect { it }
        List target = Collections.synchronizedList(new ArrayList())
        AtomicReference<Throwable> errorRef = new AtomicReference<>()
        List<Thread> threads = []

        2.times {
            threads << Thread.start {
                try {
                    BatchResult result = runLoop(AsyncForEach.of(source)
                            .concurrency(4)
                            .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                            .each(asBody { iteration, completion ->
                                target.add(iteration.item())
                                completion.success(null)
                            }))
                    assert result.successCount() == source.size()
                } catch (Throwable t) {
                    errorRef.compareAndSet(null, t)
                }
            }
        }
        threads*.join()

        assert errorRef.get() == null
        assert target.size() == 2 * source.size()
        assert target.every { it != null }
    }

    void testRunAll() {
        List items = (0..30).collect { it }
        AtomicInteger count = new AtomicInteger()
        AtomicInteger completionCount = new AtomicInteger()

        AsyncForEach.of(items)
                .concurrency(10)
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each(asBody { iteration, completion ->
                    logger.debug(String.format("item %s completed", iteration.item()))
                    count.incrementAndGet()
                    completion.success(null)
                })
                .execute(new AsyncForEachCompletion(null) {
                    @Override
                    void completed(BatchResult result) {
                        completionCount.incrementAndGet()
                    }
                })

        retryInSecs {
            assert count.get() == items.size()
            assert completionCount.get() == 1
        }
    }

    static void testRunAllWithConcurrencyZero() {
        try {
            AsyncForEach.of([1]).concurrency(0)
            assert false: "zero concurrency must be rejected"
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static BatchResult runLoop(AsyncForEach forEach) {
        FutureCompletion future = new FutureCompletion(null)
        AtomicReference<BatchResult> resultRef = new AtomicReference<>()

        forEach.execute(new AsyncForEachCompletion(future) {
            @Override
            void completed(BatchResult result) {
                resultRef.set(result)
                future.success()
            }
        })

        future.await(TIME_OUT)
        assert future.isSuccess()
        assert resultRef.get() != null
        return resultRef.get()
    }

    private static AsyncForEach.Body asBody(Closure closure) {
        return closure as AsyncForEach.Body
    }

    private static void await(CountDownLatch latch) {
        try {
            assert latch.await(TIME_OUT, TimeUnit.MILLISECONDS)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt()
            throw new AssertionError(e)
        }
    }
}
