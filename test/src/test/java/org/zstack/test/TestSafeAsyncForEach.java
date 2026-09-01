package org.zstack.test;

import org.zstack.core.asyncbatch.AsyncForEach;
import org.zstack.core.asyncbatch.AsyncForEachCompletion;
import org.zstack.core.asyncbatch.BatchResult;
import org.zstack.core.asyncbatch.FailurePolicy;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * Safety tests for AsyncForEach callbacks and exception handling.
 *
 * <p>This is the AsyncForEach counterpart of {@link TestSafeWhile}. The
 * assertions deliberately use Java assertions because the class is invoked by
 * the integration test case in the same way as TestSafeWhile.</p>
 */
public class TestSafeAsyncForEach {
    public void testSafeAsyncForEach() {
        // An exception raised by the loop body is converted to an item failure,
        // while CONTINUE_ON_FAILURE still lets the remaining items run.
        BatchResult<Integer, Void> result = execute(AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> {
                    throw new OperationFailureException(operr(
                            ORG_ZSTACK_TEST_10001, "on purpose %d", iteration.item()));
                }));

        assert result.failedCount() == 3 : "errors:" + result.getErrors();
        assert result.getErrors().stream().allMatch(it -> it.getDetails().startsWith("on purpose"));

        // An item completion is single-shot. A later completion and the
        // exception raised afterwards must not replace the first error.
        BatchResult<Integer, Void> result2 = execute(AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> {
                    completion.fail(operr(ORG_ZSTACK_TEST_10002, "on purpose %d", iteration.item()));
                    completion.fail(operr(ORG_ZSTACK_TEST_10003,
                            "I should not be in error list %d", iteration.item()));
                    throw new OperationFailureException(operr(ORG_ZSTACK_TEST_10004,
                            "I should not be in error list either %d", iteration.item()));
                }));

        assert result2.failedCount() == 3 : "errors:" + result2.getErrors();
        assert result2.getErrors().stream().allMatch(it -> it.getDetails().startsWith("on purpose"));

        // Exceptions from the final completion callback are isolated by
        // AsyncForEach; the callback is not retried as a batch failure.
        FutureCompletion callbackFuture = new FutureCompletion(null);
        AtomicInteger callbackCount = new AtomicInteger();
        AtomicReference<BatchResult<Integer, Void>> callbackResult = new AtomicReference<>();
        AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((iteration, completion) -> completion.success(null))
                .execute(new AsyncForEachCompletion<Integer, Void>(null) {
                    @Override
                    public void completed(BatchResult<Integer, Void> returnValue) {
                        callbackResult.set(returnValue);
                        callbackCount.incrementAndGet();
                        callbackFuture.success();
                        throw new OperationFailureException(operr(ORG_ZSTACK_TEST_10005,
                                "completion, on purpose"));
                    }
                });

        callbackFuture.await();
        assert callbackFuture.isSuccess();
        assert callbackCount.get() == 1;
        assert callbackResult.get().successCount() == 3;

        // A completion callback exception is forwarded to an AsyncForEach item's
        // outer completion, just like WhileDoneCompletion.done().
        BatchResult<Integer, Void> callbackFailureResult = execute(AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((outerIteration, outerCompletion) ->
                        AsyncForEach.<Integer, Void>of(Arrays.asList(1))
                                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                                .each((innerIteration, innerCompletion) -> innerCompletion.success(null))
                                .execute(new AsyncForEachCompletion<Integer, Void>(outerCompletion) {
                                    @Override
                                    public void completed(BatchResult<Integer, Void> returnValue) {
                                        throw new OperationFailureException(operr(ORG_ZSTACK_TEST_10009,
                                                "completion, on purpose"));
                                    }
                                })));

        assert callbackFailureResult.failedCount() == 3 : "errors:" + callbackFailureResult.getErrors();
        assert callbackFailureResult.getErrors().stream().allMatch(it ->
                it.getDetails().equals("completion, on purpose"));

        // A nested AsyncForEach can complete its parent item from its batch
        // callback. Duplicate parent completion and a callback exception are
        // both isolated, leaving one first error for each outer item.
        BatchResult<Integer, Void> nestedResult = execute(AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3))
                .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                .each((outerIteration, outerCompletion) -> {
                    AsyncForEach.<Integer, Void>of(Arrays.asList(1, 2, 3))
                            .failurePolicy(FailurePolicy.CONTINUE_ON_FAILURE)
                            .each((innerIteration, innerCompletion) -> innerCompletion.success(null))
                            .execute(new AsyncForEachCompletion<Integer, Void>(null) {
                                @Override
                                public void completed(BatchResult<Integer, Void> returnValue) {
                                    outerCompletion.fail(operr(ORG_ZSTACK_TEST_10006, "on purpose"));
                                    outerCompletion.fail(operr(ORG_ZSTACK_TEST_10007,
                                            "I should not be in error list"));
                                    throw new OperationFailureException(operr(ORG_ZSTACK_TEST_10008,
                                            "I should not be in error list either."));
                                }
                            });
                }));

        assert nestedResult.failedCount() == 3 : "errors:" + nestedResult.getErrors();
        assert nestedResult.getErrors().stream().allMatch(it -> it.getDetails().startsWith("on purpose"));
    }

    private static <I, R> BatchResult<I, R> execute(AsyncForEach<I, R> forEach) {
        FutureCompletion future = new FutureCompletion(null);
        AtomicReference<BatchResult<I, R>> result = new AtomicReference<>();
        forEach.execute(new AsyncForEachCompletion<I, R>(future) {
            @Override
            public void completed(BatchResult<I, R> returnValue) {
                result.set(returnValue);
                future.success();
            }
        });

        future.await();
        assert future.isSuccess() : "AsyncForEach failed";
        assert result.get() != null;
        return result.get();
    }
}
