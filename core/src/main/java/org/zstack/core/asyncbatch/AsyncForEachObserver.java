package org.zstack.core.asyncbatch;

/**
 * Receives lifecycle notifications from an {@link AsyncForEach} execution.
 *
 * <p>Notifications can be concurrent when the foreach has a concurrency greater than one.
 * Implementations must be thread-safe and must not include sensitive item data in telemetry
 * labels or logs. Exceptions raised by an observer are isolated from the foreach execution.</p>
 */
public interface AsyncForEachObserver<I, R> {
    default void onStart(int itemCount, int concurrency, FailurePolicy failurePolicy) {
    }

    default void onItemStarted(Iteration<I> iteration) {
    }

    default void onItemCompleted(ItemResult<I, R> result) {
    }

    default void onComplete(BatchResult<I, R> result) {
    }
}
