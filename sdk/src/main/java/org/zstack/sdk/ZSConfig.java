package org.zstack.sdk;

import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/12/9.
 */
public class ZSConfig {
    String hostname = "localhost";
    int port = 8080;
    long defaultPollingTimeout = TimeUnit.HOURS.toMillis(3);
    long defaultPollingInterval = TimeUnit.SECONDS.toMillis(1);

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public long getDefaultPollingTimeout() {
        return defaultPollingTimeout;
    }

    public long getDefaultPollingInterval() {
        return defaultPollingInterval;
    }

    public static class Builder {
        ZSConfig config = new ZSConfig();

        public Builder setHostname(String hostname) {
            config.hostname = hostname;
            return this;
        }

        public Builder setPort(int port) {
            config.port = port;
            return this;
        }

        public Builder setDefaultPollingTimeout(long value, TimeUnit unit) {
            config.defaultPollingTimeout = unit.toMillis(value);
            return this;
        }

        public Builder setDefaultPollingInterval(long value, TimeUnit unit) {
            config.defaultPollingInterval = unit.toMillis(value);
            return this;
        }

        public ZSConfig build() {
            return config;
        }
    }
}
