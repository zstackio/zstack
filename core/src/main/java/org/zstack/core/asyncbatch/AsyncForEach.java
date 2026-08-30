package org.zstack.core.asyncbatch;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class AsyncForEach<I, R> {
    private static final CLogger logger = Utils.getLogger(AsyncForEach.class);

    @FunctionalInterface
    public interface Body<I, R> {
        void execute(Iteration<I> iteration, AsyncItemCompletion<R> completion);
    }

    private final List<I> items;
    private int concurrency = 1;
    private FailurePolicy failurePolicy;
    private Body<I, R> body;
    private AsyncForEachObserver<I, R> observer;
    private boolean executed;

    private AsyncForEach(Collection<I> items) {
        if (items == null) {
            throw new IllegalArgumentException("items cannot be null");
        }
        this.items = new ArrayList<>(items);
    }

    public static <I, R> AsyncForEach<I, R> of(Collection<I> items) {
        return new AsyncForEach<>(items);
    }

    public AsyncForEach<I, R> concurrency(int concurrency) {
        if (concurrency <= 0) {
            throw new IllegalArgumentException(String.format("concurrency must be greater than zero, got %s", concurrency));
        }
        checkNotExecuted();
        this.concurrency = concurrency;
        return this;
    }

    public AsyncForEach<I, R> failurePolicy(FailurePolicy failurePolicy) {
        if (failurePolicy == null) {
            throw new IllegalArgumentException("failurePolicy cannot be null");
        }
        checkNotExecuted();
        this.failurePolicy = failurePolicy;
        return this;
    }

    public AsyncForEach<I, R> each(Body<I, R> body) {
        try {
            if (body == null) {
                throw new IllegalArgumentException("body cannot be null");
            }
            checkNotExecuted();
            this.body = body;
            return this;
        } catch (RuntimeException | Error e) {
            logger.error("AsyncForEach failed to configure loop body", e);
            throw e;
        }
    }

    public AsyncForEach<I, R> observer(AsyncForEachObserver<I, R> observer) {
        if (observer == null) {
            throw new IllegalArgumentException("observer cannot be null");
        }
        checkNotExecuted();
        this.observer = observer;
        return this;
    }

    /**
     * Starts the batch execution.
     *
     * <p>{@link AsyncForEachCompletion#completed(BatchResult)} is called when all in-flight items
     * have reached a terminal state and a {@link BatchResult} is available. Item failures are
     * reported in that result. Exceptions from the completion callback and failures that prevent
     * producing a batch result are forwarded to the completion's backups by the async backup
     * mechanism.</p>
     *
     * @throws IllegalArgumentException if {@code completion} is {@code null}
     */
    public synchronized void execute(AsyncForEachCompletion<I, R> completion) {
        if (completion == null) {
            throw new IllegalArgumentException("completion cannot be null");
        }
        try {
            checkNotExecuted();
            if (failurePolicy == null) {
                throw new IllegalStateException("failurePolicy must be specified before execute()");
            }
            if (body == null) {
                throw new IllegalStateException("each() must be called before execute()");
            }
            executed = true;
            logger.debug(String.format("AsyncForEach started [items:%s, concurrency:%s, failurePolicy:%s]",
                    items.size(), concurrency, failurePolicy));
            observe("start", () -> observer.onStart(items.size(), concurrency, failurePolicy));
            new Scheduler(completion).drain();
        } catch (Throwable t) {
            logger.error("AsyncForEach failed before scheduling items", t);
            failCompletion(completion, toErrorCode(t));
        }
    }

    private void checkNotExecuted() {
        if (executed) {
            throw new IllegalStateException("AsyncForEach can only be executed once");
        }
    }

    private final class Scheduler {
        private final Object lock = new Object();
        private final AtomicInteger drainWork = new AtomicInteger(0);
        private final AsyncForEachCompletion<I, R> completion;
        private final List<ItemResult<I, R>> results;

        private int nextIndex;
        private int inFlight;
        private boolean finished;
        private BatchTermination stopTermination;
        private String stopReason;

        private Scheduler(AsyncForEachCompletion<I, R> completion) {
            this.completion = completion;
            this.results = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                results.add(null);
            }
        }

        private void drain() {
            try {
                if (drainWork.getAndIncrement() != 0) {
                    return;
                }

                int work = 1;
                do {
                    while (true) {
                        int index = takeNextIndex();
                        if (index < 0) {
                            BatchResult<I, R> result = finishIfReady();
                            if (result != null) {
                                notifyBatchCompleted(result);
                            }
                            break;
                        }
                        executeItem(index);
                    }

                    work = drainWork.addAndGet(-work);
                } while (work != 0);
            } catch (Throwable t) {
                failScheduler(t);
            }
        }

        private int takeNextIndex() {
            synchronized (lock) {
                if (finished || stopTermination != null || inFlight >= concurrency || nextIndex >= items.size()) {
                    return -1;
                }

                int index = nextIndex++;
                inFlight++;
                return index;
            }
        }

        private void executeItem(int index) {
            I item = items.get(index);
            Iteration<I> iteration = new Iteration<>(index, item);
            AsyncItemCompletion<R> itemCompletion = new AsyncItemCompletion<R>(completion) {
                @Override
                protected void done(ItemStatus status, R result, ErrorCode error, String reason, boolean breakLoop) {
                    itemDone(index, status, result, error, reason, breakLoop);
                }
            };

            try {
                logger.trace(String.format("AsyncForEach item started [index:%s]", index));
                observe("item-start", () -> observer.onItemStarted(iteration));
                body.execute(iteration, itemCompletion);
            } catch (Throwable t) {
                try {
                    itemCompletion.fail(toErrorCode(t));
                } catch (Throwable completionError) {
                    logger.error(String.format("AsyncForEach failed to complete item [index:%s]", index), completionError);
                    failItem(index, completionError);
                }
            }
        }

        private void itemDone(int index, ItemStatus status, R result, ErrorCode error, String reason, boolean breakLoop) {
            try {
                ItemResult<I, R> itemResult;
                synchronized (lock) {
                    if (finished || results.get(index) != null) {
                        return;
                    }

                    I item = items.get(index);
                    if (status == ItemStatus.SUCCEEDED) {
                        itemResult = ItemResult.succeeded(index, item, result);
                    } else if (status == ItemStatus.FAILED) {
                        ErrorCode failure = error == null ? toErrorCode(
                                new IllegalStateException("item completion returned a null error code")) : error;
                        itemResult = ItemResult.failed(index, item, failure);
                    } else {
                        itemResult = ItemResult.skipped(index, item, reason);
                    }
                    results.set(index, itemResult);
                    inFlight--;

                    if (stopTermination == null) {
                        if (breakLoop) {
                            stopTermination = BatchTermination.STOPPED_BY_ITEM;
                            stopReason = reason;
                        } else if (status == ItemStatus.FAILED && failurePolicy == FailurePolicy.STOP_ON_FAILURE) {
                            stopTermination = BatchTermination.STOPPED_ON_FAILURE;
                            stopReason = itemResult.getError().getReadableDetails();
                        }
                    }
                }
                logger.trace(String.format("AsyncForEach item completed [index:%s, status:%s]", index, status));
                observe("item-complete", () -> observer.onItemCompleted(itemResult));
            } catch (Throwable t) {
                logger.error(String.format("AsyncForEach failed while recording item completion [index:%s]", index), t);
                failItem(index, t);
            }
            drain();
        }

        private void failItem(int index, Throwable throwable) {
            synchronized (lock) {
                if (finished || results.get(index) != null) {
                    return;
                }

                ErrorCode error = toErrorCode(throwable);
                results.set(index, ItemResult.failed(index, items.get(index), error));
                inFlight--;
                if (stopTermination == null) {
                    stopTermination = BatchTermination.STOPPED_ON_FAILURE;
                    stopReason = error.getReadableDetails();
                }
            }
            drain();
        }

        private BatchResult<I, R> finishIfReady() {
            synchronized (lock) {
                if (finished || inFlight != 0 || stopTermination == null && nextIndex < items.size()) {
                    return null;
                }

                for (int i = nextIndex; i < items.size(); i++) {
                    results.set(i, ItemResult.notStarted(i, items.get(i)));
                }
                finished = true;
                BatchTermination termination = stopTermination == null
                        ? BatchTermination.ALL_ITEMS_PROCESSED
                        : stopTermination;
                return new BatchResult<>(termination, new ArrayList<>(results), stopReason);
            }
        }

        private void notifyBatchCompleted(BatchResult<I, R> result) {
            logger.debug(String.format("AsyncForEach completed [termination:%s, success:%s, failed:%s, skipped:%s, notStarted:%s]",
                    result.getTermination(), result.successCount(), result.failedCount(),
                    result.skippedCount(), result.notStartedCount()));
            observe("complete", () -> observer.onComplete(result));
            try {
                completion.completed(result);
            } catch (Throwable t) {
                logger.error("AsyncForEach completion callback failed", t);
            }
        }

        private void failScheduler(Throwable throwable) {
            synchronized (lock) {
                if (finished) {
                    return;
                }
                finished = true;
            }
            logger.error("AsyncForEach scheduler failed", throwable);
            failCompletion(completion, toErrorCode(throwable));
        }

        private ErrorCode toErrorCode(Throwable t) {
            return AsyncForEach.toErrorCode(t,
                    "an internal error happened while executing AsyncForEach item");
        }
    }

    private void observe(String event, Runnable callback) {
        if (observer == null) {
            return;
        }
        try {
            callback.run();
        } catch (Throwable t) {
            logger.warn(String.format("AsyncForEach observer failed [event:%s]", event), t);
        }
    }

    private ErrorCode toErrorCode(Throwable throwable) {
        return toErrorCode(throwable, "an internal error happened while executing AsyncForEach");
    }

    private static ErrorCode toErrorCode(Throwable throwable, String description) {
        if (throwable instanceof OperationFailureException && ((OperationFailureException) throwable).getErrorCode() != null) {
            return ((OperationFailureException) throwable).getErrorCode();
        }

        ErrorCode error = new ErrorCode();
        error.setCode(SysErrors.INTERNAL.toString());
        error.setDescription(description);
        error.setDetails(throwable.getMessage() == null ? throwable.getClass().getName() : throwable.getMessage());
        return error;
    }

    private static void failCompletion(AsyncForEachCompletion<?, ?> completion, ErrorCode error) {
        // AsyncBackupAspect intercepts this join point and forwards the failure to backups.
    }
}
