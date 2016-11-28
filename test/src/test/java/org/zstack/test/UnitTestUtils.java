package org.zstack.test;

import org.zstack.utils.ShellUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class UnitTestUtils {
    public static void runTestCase(Class<?> clazz, String param) {
        if (param != null) {
            ShellUtils.runVerbose(String.format("mvn test -Dtest=%s %s", clazz.getSimpleName(), param), System.getProperty("user.dir"), false);
        } else {
            ShellUtils.runVerbose(String.format("mvn test -Dtest=%s", clazz.getSimpleName()), System.getProperty("user.dir"), false);
        }
    }

    public static void runTestCase(Class<?> clazz) {
        runTestCase(clazz, null);
    }

    public static void sleepRetry(Callable runnable, int interval, int retries, Class catchedException) {
        while (retries > 0) {
            try {
                runnable.call();
                return;
            } catch (Throwable t) {
                if (catchedException.isAssignableFrom(t.getClass())) {
                    retries--;
                    try {
                        TimeUnit.SECONDS.sleep(interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    throw new RuntimeException(t);
                }
            }
        }
    }

    public static void sleepRetry(Callable runnable, int retries) {
        sleepRetry(runnable, 1, retries, Throwable.class);
    }
}
