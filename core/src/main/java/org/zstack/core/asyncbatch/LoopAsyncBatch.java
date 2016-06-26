package org.zstack.core.asyncbatch;

import org.zstack.header.core.AsyncBackup;
import org.zstack.utils.DebugUtils;

import java.util.Collection;

/**
 * Created by xing5 on 2016/6/26.
 */
public abstract class LoopAsyncBatch<T> extends AsyncBatch {
    public LoopAsyncBatch(AsyncBackup... backups) {
        super(backups);
    }

    public LoopAsyncBatch() {
    }

    protected abstract Collection<T> collect();

    protected abstract AsyncBatchRunner forEach(T item);

    @Override
    protected abstract void done();

    @Override
    protected final void setup() {
        Collection<T> collection = collect();
        DebugUtils.Assert(collection != null, "collect() cannot return null");

        for (T item : collection) {
            batch(forEach(item));
        }
    }
}
