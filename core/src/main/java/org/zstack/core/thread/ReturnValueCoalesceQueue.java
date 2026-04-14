package org.zstack.core.thread;

import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.List;

/**
 * A coalesce queue for requests that expect a return value.
 *
 * @param <T> Request Item Type
 * @param <R> Batch Execution Result Type
 * @param <V> Single Request Result Type
 */
public abstract class ReturnValueCoalesceQueue<T, R, V> extends AbstractCoalesceQueue<T, R, V> {

    public void submit(String syncSignature, T item, ReturnValueCompletion<V> completion) {
        submitRequest(syncSignature, item, completion);
    }

    protected abstract void executeBatch(List<T> items, ReturnValueCompletion<R> completion);

    @Override
    protected final void executeBatch(List<T> items, AbstractCompletion batchCompletion) {
        executeBatch(items, (ReturnValueCompletion<R>) batchCompletion);
    }

    @Override
    protected final AbstractCompletion createBatchCompletion(String syncSignature, List<PendingRequest> requests, SyncTaskChain chain) {
        return new ReturnValueCompletion<R>(chain) {
            @Override
            public void success(R batchResult) {
                handleSuccess(syncSignature, requests, batchResult, chain);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                handleFailure(syncSignature, requests, errorCode, chain);
            }
        };
    }
}
