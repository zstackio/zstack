package org.zstack.utils;

import org.zstack.utils.logging.CLogger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 */
public class TimeUtils {

    public static String TIME_UNIT_WEEKS = "WEEKS";
    public static String TIME_UNIT_MONTHS = "MONTHS";

    private static CLogger logger = Utils.getLogger(TimeUtils.class);

    public static boolean loopExecuteUntilTimeoutIgnoreExceptionAndReturn(long period, long interval, TimeUnit unit, Callable<Boolean> runnable) {
        long count = 0;
        while (count < period) {
            try {
                if (runnable.call()) {
                    return true;
                }

                unit.sleep(interval);
            } catch (Throwable t) {
                logger.debug(String.format("%s, after %s ms timeout", t.getMessage(), period-count));
            }
            count += interval;
        }

        return false;
    }

    public static void loopExecuteUntilTimeoutIgnoreException(long period, long interval, TimeUnit unit, Callable<Boolean> runnable) {
        if (!loopExecuteUntilTimeoutIgnoreExceptionAndReturn(period, interval, unit, runnable)) {
            throw new RuntimeException(String.format("timeout after %s seconds", period));
        }
    }

    public static boolean isValidTimeFormat(String time) {
        try {
            parseTimeInMillis(time);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
            } else if (time.endsWith("w")) {
                time = StringDSL.stripEnd(time, "w");
                return TimeUnit.DAYS.toMillis(Long.valueOf(time) * 7);
            } else if (time.endsWith("W")) {
                time = StringDSL.stripEnd(time, "W");
                return TimeUnit.DAYS.toMillis(Long.valueOf(time) * 7);
            } else if (time.endsWith("y")) {
                time = StringDSL.stripEnd(time, "y");
                return TimeUnit.DAYS.toMillis(Long.valueOf(time) * 365);
            } else if (time.endsWith("Y")) {
                time = StringDSL.stripEnd(time, "Y");
                return TimeUnit.DAYS.toMillis(Long.valueOf(time) * 365);
            } else {
                throw new NumberFormatException();
            }
        }
    }

    public static long parseTimeToSeconds(String time){
        if (time.equals(TimeUtils.TIME_UNIT_WEEKS)) {
            return TimeUnit.DAYS.toSeconds(7);
        }
        if (time.equals(TimeUtils.TIME_UNIT_MONTHS)) {
            return TimeUnit.DAYS.toSeconds(30);
        }
        return TimeUnit.valueOf(time).toSeconds((long) 1);
    }

    public static long parseTimeToMillis(String time){
        if (time.equals(TimeUtils.TIME_UNIT_WEEKS)) {
            return TimeUnit.DAYS.toMillis(7);
        }
        if (time.equals(TimeUtils.TIME_UNIT_MONTHS)) {
            return TimeUnit.DAYS.toMillis(30);
        }
        return TimeUnit.valueOf(time).toMillis((long) 1);
    }

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static boolean isValidTimestampFormat(String timestamp) {
        try {
            df.parse(timestamp);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static boolean isValidTimestampFormat(String timestamp, String dformat) {
        try {
            DateFormat dateFormat = new SimpleDateFormat(dformat);
            dateFormat.parse(timestamp);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static long parseFormatStringToTimeStamp(String timestamp, String dformat) {
        try {
            DateFormat dateFormat = new SimpleDateFormat(dformat);
            return dateFormat.parse(timestamp).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public static long parseFormatStringToTimeStamp(String timestamp) {
        try {
            return df.parse(timestamp).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public static String getCurrentTimeStamp() {
        return df.format(new Date(System.currentTimeMillis()));
    }

    public static String getCurrentTimeStamp(String dformat) {
        DateFormat dateFormat = new SimpleDateFormat(dformat);
        return dateFormat.format(new Date(System.currentTimeMillis()));
    }
}
