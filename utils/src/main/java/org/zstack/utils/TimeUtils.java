package org.zstack.utils;

import org.zstack.utils.logging.CLogger;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 */
public class TimeUtils {
    private static CLogger logger = Utils.getLogger(TimeUtils.class);

    public static void loopExecuteUntilTimeoutIgnoreException(long period, long interval, TimeUnit unit, Callable<Boolean> runnable) {
        long count = 0;
        while (count < period) {
            try {
                if (runnable.call()) {
                    return;
                }

                unit.sleep(interval);
            } catch (Throwable t) {
                logger.debug(String.format("%s, after %s ms timeout", t.getMessage(), period-count));
            }
            count += interval;
        }

        throw new RuntimeException(String.format("timeout after %s seconds", period));
    }
}
