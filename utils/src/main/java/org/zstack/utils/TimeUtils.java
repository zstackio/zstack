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

    public static long parseTimeInMillis(String time) {
        try {
            return Long.valueOf(time);
        } catch (NumberFormatException e) {
            if (time.endsWith("s")) {
                time = StringDSL.stripEnd(time, "s");
                return TimeUnit.SECONDS.toMillis(Long.valueOf(time));
            } else if (time.endsWith("S")) {
                time = StringDSL.stripEnd(time, "S");
                return TimeUnit.SECONDS.toMillis(Long.valueOf(time));
            } else if (time.endsWith("m")) {
                time = StringDSL.stripEnd(time, "m");
                return TimeUnit.MINUTES.toMillis(Long.valueOf(time));
            } else if (time.endsWith("M")) {
                time = StringDSL.stripEnd(time, "M");
                return TimeUnit.MINUTES.toMillis(Long.valueOf(time));
            } else if (time.endsWith("h")) {
                time = StringDSL.stripEnd(time, "h");
                return TimeUnit.HOURS.toMillis(Long.valueOf(time));
            } else if (time.endsWith("H")) {
                time = StringDSL.stripEnd(time, "H");
                return TimeUnit.HOURS.toMillis(Long.valueOf(time));
            } else if (time.endsWith("d")) {
                time = StringDSL.stripEnd(time, "d");
                return TimeUnit.DAYS.toMillis(Long.valueOf(time));
            } else if (time.endsWith("D")) {
                time = StringDSL.stripEnd(time, "D");
                return TimeUnit.DAYS.toMillis(Long.valueOf(time));
            } else {
                throw new NumberFormatException();
            }
        }
    }
}
