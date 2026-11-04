package org.zstack.core.plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Holds the REST requests of a feature plugin while the management node switches the plugin to another version, so
 * that callers wait a moment instead of getting a 404. A plugin's REST paths start with /&lt;plugin id&gt;/.
 */
public class FeaturePluginGate {
    static final long WAIT_MILLIS = TimeUnit.SECONDS.toMillis(5);

    private static final Map<String, CountDownLatch> switching = new ConcurrentHashMap<>();

    static void open(String pluginId) {
        switching.put(pluginId, new CountDownLatch(1));
    }

    static void close(String pluginId) {
        CountDownLatch latch = switching.remove(pluginId);
        if (latch != null) {
            latch.countDown();
        }
    }

    /**
     * Waits while the plugin owning the REST path is switched to another version.
     *
     * @return the id of the plugin still switching when the wait ends, null when the request can go on
     */
    public static String awaitPath(String path) {
        if (switching.isEmpty() || path == null) {
            return null;
        }
        for (Map.Entry<String, CountDownLatch> e : switching.entrySet()) {
            String prefix = "/" + e.getKey();
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                try {
                    return e.getValue().await(WAIT_MILLIS, TimeUnit.MILLISECONDS) ? null : e.getKey();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return e.getKey();
                }
            }
        }
        return null;
    }
}
