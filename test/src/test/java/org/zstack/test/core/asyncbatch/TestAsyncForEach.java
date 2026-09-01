package org.zstack.test.core.asyncbatch;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.core.asyncbatch.AsyncForEach;
import org.zstack.core.asyncbatch.AsyncForEachCompletion;
import org.zstack.core.asyncbatch.AsyncForEachObserver;
import org.zstack.core.asyncbatch.BatchResult;
import org.zstack.core.asyncbatch.BatchTermination;
import org.zstack.core.asyncbatch.FailurePolicy;
import org.zstack.core.asyncbatch.ItemResult;
import org.zstack.core.asyncbatch.ItemStatus;
import org.zstack.core.asyncbatch.Iteration;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TestAsyncForEach {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(5);

    @Test
    public void testSequentialExecutionKeepsInputOrder() {
        List<Integer> visited = new ArrayList<>();
        AsyncForEach<Integer, Integer> forEach = AsyncForEach.<Integer, Integer>of(Arrays.asList(1, 2, 3))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> {
                    visited.add(iteration.item());
                    Assert.assertEquals(visited.size() - 1, iteration.index());
                    completion.success(iteration.item() * 10);
                });

        BatchResult<Integer, Integer> result = execute(forEach);

        Assert.assertEquals(Arrays.asList(1, 2, 3), visited);
        Assert.assertEquals(BatchTermination.ALL_ITEMS_PROCESSED, result.getTermination());
        Assert.assertTrue(result.isAllSucceeded());
        Assert.assertFalse(result.isAllFailed());
        Assert.assertFalse(result.isPartiallySucceeded());
        Assert.assertFalse(result.isNoSuccess());
        Assert.assertEquals(Integer.valueOf(10), result.getItemResults().get(0).getResult());
        Assert.assertEquals(Integer.valueOf(20), result.getItemResults().get(1).getResult());
        Assert.assertEquals(Integer.valueOf(30), result.getItemResults().get(2).getResult());
    }

    @Test
    public void testConcurrencyIsBounded() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(6);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        CountDownLatch firstWave = new CountDownLatch(3);

        try {
            AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3, 4, 5, 6))
                    .concurrency(3)
                    .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                    .each((iteration, completion) -> executor.submit(() -> {
                        int current = active.incrementAndGet();
                        maxActive.updateAndGet(previous -> Math.max(previous, current));
                        firstWave.countDown();
                        await(firstWave);
                        active.decrementAndGet();
                        completion.success(null);
                    }));

            BatchResult<Integer, Void> result = execute(forEach);

            Assert.assertEquals(3, maxActive.get());
            Assert.assertEquals(6, result.successCount());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testContinueOnFailureProcessesRemainingItems() {
        List<Integer> visited = new ArrayList<>();
        ErrorCode error = error("item 2 failed");
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> {
                    visited.add(iteration.item());
                    if (iteration.item() == 2) {
                        completion.fail(error);
                    } else {
                        completion.success(null);
                    }
                });

        BatchResult<Integer, Void> result = execute(forEach);

        Assert.assertEquals(Arrays.asList(1, 2, 3), visited);
        Assert.assertEquals(BatchTermination.ALL_ITEMS_PROCESSED, result.getTermination());
        Assert.assertEquals(2, result.successCount());
        Assert.assertEquals(1, result.failedCount());
        Assert.assertTrue(result.isPartiallySucceeded());
        Assert.assertSame(error, result.getErrors().get(0));
    }

    @Test
    public void testStopOnFailureMarksRemainingItemsNotStarted() {
        ErrorCode error = error("stop here");
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3, 4))
                .failurePolicy(FailurePolicy.STOP_ON_FAILURE)
                .each((iteration, completion) -> {
                    if (iteration.item() == 2) {
                        completion.fail(error);
                    } else {
                        completion.success(null);
                    }
                });

        BatchResult<Integer, Void> result = execute(forEach);

        Assert.assertEquals(BatchTermination.STOPPED_ON_FAILURE, result.getTermination());
        Assert.assertEquals(ItemStatus.SUCCEEDED, result.getItemResults().get(0).getStatus());
        Assert.assertEquals(ItemStatus.FAILED, result.getItemResults().get(1).getStatus());
        Assert.assertEquals(ItemStatus.NOT_STARTED, result.getItemResults().get(2).getStatus());
        Assert.assertEquals(ItemStatus.NOT_STARTED, result.getItemResults().get(3).getStatus());
        Assert.assertFalse(result.isAllSucceeded());
        Assert.assertFalse(result.isAllFailed());
        Assert.assertTrue(result.isPartiallySucceeded());
        Assert.assertFalse(result.isNoSuccess());
    }

    @Test
    public void testNoSuccessWhenFailureStopsBeforeRemainingItems() {
        ErrorCode error = error("stop before remaining items");
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2))
                .failurePolicy(FailurePolicy.STOP_ON_FAILURE)
                .each((iteration, completion) -> completion.fail(error));

        BatchResult<Integer, Void> result = execute(forEach);

        Assert.assertEquals(0, result.successCount());
        Assert.assertEquals(1, result.failedCount());
        Assert.assertEquals(1, result.notStartedCount());
        Assert.assertFalse(result.isAllSucceeded());
        Assert.assertFalse(result.isAllFailed());
        Assert.assertFalse(result.isPartiallySucceeded());
        Assert.assertTrue(result.isNoSuccess());
    }

    @Test
    public void testStopOnFailureWaitsForInFlightItems() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch secondItemStarted = new CountDownLatch(1);
        CountDownLatch failureSent = new CountDownLatch(1);
        CountDownLatch allowSecondItemToComplete = new CountDownLatch(1);
        CountDownLatch batchDone = new CountDownLatch(1);
        AtomicReference<BatchResult<Integer, Void>> resultRef = new AtomicReference<>();

        try {
            AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3, 4))
                    .concurrency(2)
                    .failurePolicy(FailurePolicy.STOP_ON_FAILURE)
                    .each((iteration, completion) -> {
                        if (iteration.index() == 0) {
                            executor.submit(() -> {
                                await(secondItemStarted);
                                completion.fail(error("stop here"));
                                failureSent.countDown();
                            });
                        } else {
                            executor.submit(() -> {
                                secondItemStarted.countDown();
                                await(allowSecondItemToComplete);
                                completion.success(null);
                            });
                        }
                    })
                    .execute(completion(resultRef, batchDone));

            Assert.assertTrue(failureSent.await(TIMEOUT, TimeUnit.MILLISECONDS));
            Assert.assertEquals(1, batchDone.getCount());
            allowSecondItemToComplete.countDown();
            Assert.assertTrue(batchDone.await(TIMEOUT, TimeUnit.MILLISECONDS));

            BatchResult<Integer, Void> result = resultRef.get();
            Assert.assertEquals(BatchTermination.STOPPED_ON_FAILURE, result.getTermination());
            Assert.assertEquals(ItemStatus.FAILED, result.getItemResults().get(0).getStatus());
            Assert.assertEquals(ItemStatus.SUCCEEDED, result.getItemResults().get(1).getStatus());
            Assert.assertEquals(ItemStatus.NOT_STARTED, result.getItemResults().get(2).getStatus());
            Assert.assertEquals(ItemStatus.NOT_STARTED, result.getItemResults().get(3).getStatus());
        } finally {
            allowSecondItemToComplete.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void testBreakWaitsForInFlightItems() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch secondItemStarted = new CountDownLatch(1);
        CountDownLatch breakSent = new CountDownLatch(1);
        CountDownLatch allowSecondItemToComplete = new CountDownLatch(1);
        CountDownLatch batchDone = new CountDownLatch(1);
        AtomicReference<BatchResult<Integer, String>> resultRef = new AtomicReference<>();

        try {
            AsyncForEach.<Integer, String>of(Arrays.asList(1, 2, 3, 4))
                    .concurrency(2)
                    .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                    .each((iteration, completion) -> {
                        if (iteration.index() == 0) {
                            executor.submit(() -> {
                                await(secondItemStarted);
                                completion.breakLoop("winner", "result found");
                                breakSent.countDown();
                            });
                        } else {
                            executor.submit(() -> {
                                secondItemStarted.countDown();
                                await(allowSecondItemToComplete);
                                completion.success("in-flight");
                            });
                        }
                    })
                    .execute(completion(resultRef, batchDone));

            Assert.assertTrue(breakSent.await(TIMEOUT, TimeUnit.MILLISECONDS));
            Assert.assertEquals(1, batchDone.getCount());
            allowSecondItemToComplete.countDown();
            Assert.assertTrue(batchDone.await(TIMEOUT, TimeUnit.MILLISECONDS));

            BatchResult<Integer, String> result = resultRef.get();
            Assert.assertEquals(BatchTermination.STOPPED_BY_ITEM, result.getTermination());
            Assert.assertEquals("result found", result.getStopReason());
            Assert.assertEquals("winner", result.getItemResults().get(0).getResult());
            Assert.assertEquals(ItemStatus.SUCCEEDED, result.getItemResults().get(1).getStatus());
            Assert.assertEquals(ItemStatus.NOT_STARTED, result.getItemResults().get(2).getStatus());
            Assert.assertEquals(ItemStatus.NOT_STARTED, result.getItemResults().get(3).getStatus());
        } finally {
            allowSecondItemToComplete.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void testSkipRecordsReason() {
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> {
                    if (iteration.index() == 0) {
                        completion.skip("not applicable");
                    } else {
                        completion.success(null);
                    }
                });

        BatchResult<Integer, Void> result = execute(forEach);

        Assert.assertEquals(ItemStatus.SKIPPED, result.getItemResults().get(0).getStatus());
        Assert.assertEquals("not applicable", result.getItemResults().get(0).getReason());
        Assert.assertEquals(1, result.skippedCount());
    }

    @Test
    public void testSynchronousExceptionBecomesItemFailure() {
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> {
                    if (iteration.index() == 0) {
                        throw new IllegalStateException("on purpose");
                    }
                    completion.success(null);
                });

        BatchResult<Integer, Void> result = execute(forEach);

        Assert.assertEquals(1, result.failedCount());
        Assert.assertEquals(1, result.successCount());
        Assert.assertEquals("on purpose", result.getErrors().get(0).getDetails());
    }

    @Test
    public void testErrorFromBodyBecomesItemFailure() {
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Collections.singletonList(1))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> {
                    throw new AssertionError("error from body");
                });

        BatchResult<Integer, Void> result = execute(forEach);

        Assert.assertEquals(1, result.failedCount());
        Assert.assertEquals("error from body", result.getErrors().get(0).getDetails());
    }

    @Test
    public void testOperationFailureExceptionPreservesErrorCode() {
        ErrorCode error = error("business failure");
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> {
                    if (iteration.index() == 0) {
                        throw new OperationFailureException(error);
                    }
                    completion.success(null);
                });

        BatchResult<Integer, Void> result = execute(forEach);

        Assert.assertSame(error, result.getErrors().get(0));
        Assert.assertEquals(1, result.successCount());
    }

    @Test
    public void testNullFailureIsConvertedToItemFailure() {
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Collections.singletonList(1))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> completion.fail(null));

        BatchResult<Integer, Void> result = execute(forEach);

        Assert.assertEquals(1, result.failedCount());
        Assert.assertTrue(result.isAllFailed());
        Assert.assertNotNull(result.getErrors().get(0));
    }

    @Test
    public void testObserverReceivesLifecycleAndObserverFailureIsIsolated() {
        AtomicInteger started = new AtomicInteger();
        AtomicInteger itemStarted = new AtomicInteger();
        AtomicInteger itemCompleted = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        AsyncForEachObserver<Integer, Integer> observer = new AsyncForEachObserver<Integer, Integer>() {
            @Override
            public void onStart(int itemCount, int concurrency, FailurePolicy failurePolicy) {
                Assert.assertEquals(3, itemCount);
                Assert.assertEquals(1, concurrency);
                Assert.assertEquals(FailurePolicy.CONTINUE_ON_FAILURE, failurePolicy);
                started.incrementAndGet();
                throw new RuntimeException("observer start failure");
            }

            @Override
            public void onItemStarted(Iteration<Integer> iteration) {
                if (itemStarted.incrementAndGet() == 1) {
                    throw new RuntimeException("observer failure");
                }
            }

            @Override
            public void onItemCompleted(ItemResult<Integer, Integer> result) {
                if (itemCompleted.incrementAndGet() == 1) {
                    throw new RuntimeException("observer item completion failure");
                }
            }

            @Override
            public void onComplete(BatchResult<Integer, Integer> result) {
                completed.incrementAndGet();
                throw new RuntimeException("observer completion failure");
            }
        };

        AsyncForEach<Integer, Integer> forEach = AsyncForEach.<Integer, Integer>of(Arrays.asList(1, 2, 3))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .observer(observer)
                .each((iteration, completion) -> completion.success(iteration.item() * 2));

        BatchResult<Integer, Integer> result = execute(forEach);

        Assert.assertEquals(3, result.successCount());
        Assert.assertEquals(1, started.get());
        Assert.assertEquals(3, itemStarted.get());
        Assert.assertEquals(3, itemCompleted.get());
        Assert.assertEquals(1, completed.get());
    }

    @Test
    public void testDuplicateCompletionDoesNotChangeResult() {
        AsyncForEach<Integer, String> forEach = AsyncForEach.<Integer, String>of(Collections.singletonList(1))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> {
                    completion.success("first");
                    completion.skip("second");
                });

        BatchResult<Integer, String> result = execute(forEach);

        Assert.assertEquals(ItemStatus.SUCCEEDED, result.getItemResults().get(0).getStatus());
        Assert.assertEquals("first", result.getItemResults().get(0).getResult());
    }

    @Test
    public void testEmptyItemsCompleteImmediately() {
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Collections.emptyList())
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> Assert.fail("body must not be called"));

        BatchResult<Integer, Void> result = execute(forEach);

        Assert.assertEquals(BatchTermination.ALL_ITEMS_PROCESSED, result.getTermination());
        Assert.assertEquals(0, result.totalCount());
        Assert.assertTrue(result.isEmpty());
        Assert.assertFalse(result.isAllSucceeded());
        Assert.assertFalse(result.isAllFailed());
        Assert.assertFalse(result.isPartiallySucceeded());
        Assert.assertFalse(result.isNoSuccess());
    }

    @Test
    public void testInputCollectionIsSnapshotted() {
        List<Integer> items = new ArrayList<>(Collections.singletonList(1));
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(items)
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> completion.success(null));
        items.add(2);

        BatchResult<Integer, Void> result = execute(forEach);

        Assert.assertEquals(1, result.totalCount());
        Assert.assertEquals(Integer.valueOf(1), result.getItemResults().get(0).getItem());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBatchResultItemsAreImmutable() {
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Collections.singletonList(1))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> completion.success(null));

        execute(forEach).getItemResults().clear();
    }

    @Test
    public void testExecuteValidationFailureUsesBackup() {
        FutureCompletion backup = new FutureCompletion(null);
        AsyncForEach<Integer, Void> forEach = AsyncForEach.of(Collections.singletonList(1));

        forEach.execute(new AsyncForEachCompletion<Integer, Void>(backup) {
            @Override
            public void completed(BatchResult<Integer, Void> returnValue) {
                Assert.fail("validation failure must not succeed");
            }
        });

        backup.await();
        Assert.assertFalse(backup.isSuccess());
        Assert.assertNotNull(backup.getErrorCode());
        Assert.assertEquals("SYS.1000", backup.getErrorCode().getCode());
    }

    @Test
    public void testRepeatedExecuteUsesBackup() {
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Collections.singletonList(1))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> completion.success(null));
        execute(forEach);

        FutureCompletion backup = new FutureCompletion(null);
        forEach.execute(new AsyncForEachCompletion<Integer, Void>(backup) {
            @Override
            public void completed(BatchResult<Integer, Void> returnValue) {
                Assert.fail("repeated execution must not succeed");
            }
        });

        backup.await();
        Assert.assertFalse(backup.isSuccess());
        Assert.assertNotNull(backup.getErrorCode());
    }

    @Test
    public void testCompletionCallbackExceptionDoesNotEscapeExecute() {
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Collections.singletonList(1))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> completion.success(null));

        forEach.execute(new AsyncForEachCompletion<Integer, Void>(null) {
            @Override
            public void completed(BatchResult<Integer, Void> returnValue) {
                throw new RuntimeException("completion failure");
            }
        });
    }

    @Test
    public void testCompletionCallbackExceptionFailsBackup() {
        FutureCompletion backup = new FutureCompletion(null);
        AsyncForEach<Integer, Void> forEach = AsyncForEach.<Integer, Void>of(Collections.singletonList(1))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> completion.success(null));

        forEach.execute(new AsyncForEachCompletion<Integer, Void>(backup) {
            @Override
            public void completed(BatchResult<Integer, Void> returnValue) {
                throw new OperationFailureException(error("completion failure"));
            }
        });

        backup.await();
        Assert.assertFalse(backup.isSuccess());
        Assert.assertEquals("completion failure", backup.getErrorCode().getDetails());
    }

    @Test
    public void testCompletionCallbackExceptionFailsOuterItemCompletion() {
        BatchResult<Integer, Void> result = execute(AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, outerCompletion) ->
                        AsyncForEach.<Integer, Void>of(Collections.singletonList(1))
                                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                                .each((innerIteration, innerCompletion) -> innerCompletion.success(null))
                                .execute(new AsyncForEachCompletion<Integer, Void>(outerCompletion) {
                                    @Override
                                    public void completed(BatchResult<Integer, Void> returnValue) {
                                        throw new OperationFailureException(error("completion failure"));
                                    }
                                })));

        Assert.assertEquals(3, result.failedCount());
        Assert.assertTrue(result.getErrors().stream()
                .allMatch(error -> "completion failure".equals(error.getDetails())));
    }

    @Test
    public void testCompletionCallbackExceptionForwardsThroughNestedAsyncForEachCompletionBackup() {
        FutureCompletion finalBackup = new FutureCompletion(null);
        AsyncForEachCompletion<Integer, Void> nestedCompletion =
                new AsyncForEachCompletion<Integer, Void>(finalBackup) {
                    @Override
                    public void completed(BatchResult<Integer, Void> returnValue) {
                        Assert.fail("nested completion must not be called without a batch result");
                    }
                };

        AsyncForEach.<Integer, Void>of(Collections.singletonList(1))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> completion.success(null))
                .execute(new AsyncForEachCompletion<Integer, Void>(nestedCompletion) {
                    @Override
                    public void completed(BatchResult<Integer, Void> returnValue) {
                        throw new OperationFailureException(error("nested completion failure"));
                    }
                });

        finalBackup.await();
        Assert.assertFalse(finalBackup.isSuccess());
        Assert.assertEquals("nested completion failure", finalBackup.getErrorCode().getDetails());
    }

    @Test
    public void testFailureCompletionCallbackExceptionDoesNotEscapeExecute() {
        AsyncForEach<Integer, Void> forEach = AsyncForEach.of(Collections.singletonList(1));

        forEach.execute(new AsyncForEachCompletion<Integer, Void>(null) {
            @Override
            public void completed(BatchResult<Integer, Void> returnValue) {
                Assert.fail("validation failure must not succeed");
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsNullCompletion() {
        AsyncForEach.<Integer, Void>of(Collections.singletonList(1))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> completion.success(null))
                .execute(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsNonPositiveConcurrency() {
        AsyncForEach.of(Collections.singletonList(1)).concurrency(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsNullItems() {
        AsyncForEach.of(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsNullBody() {
        AsyncForEach.<Integer, Void>of(Collections.singletonList(1)).each(null);
    }

    private static ErrorCode error(String details) {
        return new ErrorCode("TEST", "test error", details);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionError("timed out waiting for latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static <I, R> BatchResult<I, R> execute(AsyncForEach<I, R> forEach) {
        AtomicReference<BatchResult<I, R>> result = new AtomicReference<>();
        FutureCompletion completion = new FutureCompletion(null);
        forEach.execute(new AsyncForEachCompletion<I, R>(completion) {
            @Override
            public void completed(BatchResult<I, R> returnValue) {
                result.set(returnValue);
                completion.success();
            }
        });
        completion.await();
        Assert.assertTrue(completion.isSuccess());
        Assert.assertNotNull(result.get());
        return result.get();
    }

    private static <I, R> AsyncForEachCompletion<I, R> completion(
            AtomicReference<BatchResult<I, R>> result, CountDownLatch done) {
        return new AsyncForEachCompletion<I, R>(null) {
            @Override
            public void completed(BatchResult<I, R> returnValue) {
                result.set(returnValue);
                done.countDown();
            }
        };
    }
}
