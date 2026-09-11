package org.zstack.core.asyncbatch;

import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;

/**
 * Receives the terminal outcome of an {@link AsyncForEach} execution.
 *
 * <p>{@link #completed(BatchResult)} means that the executor reached a terminal state and
 * produced a batch result. It does not mean that every item succeeded; item outcomes must be
 * inspected in the returned {@link BatchResult}.</p>
 */
public abstract class AsyncForEachCompletion<I, R> extends AbstractCompletion {
    public AsyncForEachCompletion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void completed(BatchResult<I, R> result);
}
