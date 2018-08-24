package org.zstack.core.thread;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AsyncTimer implements Runnable {
    protected static final CLogger logger = Utils.getLogger(AsyncTimer.class);

    protected TimeUnit timeUnit;
    protected long period;
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private ThreadFacadeImpl.TimeoutTaskReceipt cancel;

    @Autowired
    protected ThreadFacade thdf;

    protected abstract void execute();

    public AsyncTimer(TimeUnit timeUnit, long period) {
        this.timeUnit = timeUnit;
        this.period = period;
    }

    public void start() {
        if (cancelled.get()) {
            throw new CloudRuntimeException("cannot start a cancelled timer");
        }

        cancel = thdf.submitTimeoutTask(this, timeUnit, period);
    }

    public void cancel() {
        cancelled.set(true);
        cancel.cancel();
    }

    protected void continueToRunThisTimer() {
        if (cancelled.get()) {
            return;
        }

        cancel = thdf.submitTimeoutTask(this, timeUnit, period);
    }

    @Override
    public final void run() {
        if (cancelled.get()) {
            return;
        }

        try {
            execute();
        } catch (Throwable t) {
            logger.warn(String.format("unhandled exception while executing AsyncTimer[%s]", getClass()), t);
            continueToRunThisTimer();
        }
    }
}
