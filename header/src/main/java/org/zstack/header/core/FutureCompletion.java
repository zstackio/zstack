package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.concurrent.TimeUnit;

/**
 */
public class FutureCompletion extends Completion {
    private volatile boolean success;
    private ErrorCode errorCode;
    private volatile boolean done;

    public FutureCompletion(AsyncBackup...backups) {
        super(backups);
    }

    public FutureCompletion() {
        super();
    }

    @Override
    public final synchronized void success() {
        this.success = true;
        done = true;
        notifyAll();
    }

    @Override
    public synchronized void fail(ErrorCode errorCode) {
        this.success = false;
        this.errorCode = errorCode;
        done = true;
        notifyAll();
    }

    public boolean isSuccess() {
        return success;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public synchronized void await(long timeout) {
        if (done) {
            return;
        }

        try {
            wait(timeout);
        } catch (InterruptedException e) {
            throw new CloudRuntimeException(e);
        }

        if (!done) {
            ErrorCode err = new ErrorCode();
            err.setCode(SysErrors.TIMEOUT.toString());
            err.setDetails(String.format("FutureCompletion timeout after %s seconds", TimeUnit.MILLISECONDS.toSeconds(timeout)));
            fail(err);
        }
    }

    public synchronized void await() {
        if (done) {
            return;
        }

        try {
            wait();
        } catch (InterruptedException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
