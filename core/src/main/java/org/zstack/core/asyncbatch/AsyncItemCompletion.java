package org.zstack.core.asyncbatch;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.utils.DebugUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AsyncItemCompletion<R> extends ReturnValueCompletion<R> {
    private final AtomicBoolean completed = new AtomicBoolean(false);

    AsyncItemCompletion(AsyncBackup backup) {
        super(backup);
    }

    @Override
    public final void success(R result) {
        complete(ItemStatus.SUCCEEDED, result, null, null, false);
    }

    @Override
    public final void fail(ErrorCode errorCode) {
        if (errorCode == null) {
            errorCode = new ErrorCode(SysErrors.INTERNAL.toString(),
                    "an internal error happened while completing AsyncForEach item",
                    "AsyncForEach item completion received a null error code");
        }
        complete(ItemStatus.FAILED, null, errorCode, null, false);
    }

    public final void skip(String reason) {
        complete(ItemStatus.SKIPPED, null, null, reason, false);
    }

    public final void breakLoop(R result, String reason) {
        complete(ItemStatus.SUCCEEDED, result, null, reason, true);
    }

    private void complete(ItemStatus status, R result, ErrorCode error, String reason, boolean breakLoop) {
        if (!completed.compareAndSet(false, true)) {
            DebugUtils.dumpStackTrace("AsyncItemCompletion is mistakenly completed twice");
            return;
        }
        done(status, result, error, reason, breakLoop);
    }

    protected abstract void done(ItemStatus status, R result, ErrorCode error, String reason, boolean breakLoop);
}
