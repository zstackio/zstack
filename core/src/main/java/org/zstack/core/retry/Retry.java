package org.zstack.core.retry;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.i18n;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/6/25.
 */
public abstract class Retry<T> {
    private static final CLogger logger = Utils.getLogger(Retry.class);

    protected abstract T call();

    protected String __name__;

    public T run() {
        Method m;
        try {
            m = getClass().getDeclaredMethod("call");
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        int times = 5;
        int interval = 1;
        Class[] onExceptions = {};

        RetryCondition cond = m.getAnnotation(RetryCondition.class);
        if (cond != null) {
            times = cond.times();
            interval = cond.interval();
            onExceptions = cond.onExceptions();
        }

        int count = times;

        if (__name__ == null) {
            __name__ = getClass().getName();
        }

        do {
            try {
                return call();
            } catch (Throwable t) {
                if (onExceptions.length != 0 && !TypeUtils.isTypeOf(t, onExceptions)) {
                    throw t;
                }

                try {
                    TimeUnit.SECONDS.sleep(interval);
                } catch (InterruptedException e) {
                    logger.warn(e.getMessage(), e);
                }


                logger.debug(String.format("running [%s] encounters an exception[%s], will retry %s times with the" +
                        " interval[%s]", __name__, t.getMessage(), count, interval));

                count --;

                if (count == 0) {
                    ErrorCode errorCode = new ErrorCode();
                    errorCode.setCode(SysErrors.OPERATION_ERROR.toString());
                    errorCode.setDescription(i18n("an operation[%s] fails after retrying %s times with the interval %s seconds",
                            __name__, times, interval));
                    errorCode.setDetails(t.getMessage());
                    logger.warn(errorCode.toString(), t);

                    throw t;
                }
            }
        } while (true);
    }
}
