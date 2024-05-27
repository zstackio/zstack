package org.zstack.xinfini.sdk;

import java.util.concurrent.TimeUnit;

public class XInfiniConnectConfig {
    public String hostname = "localhost";
    public int port = 443;
    long defaultPollingTimeout = TimeUnit.HOURS.toMillis(3);
    long defaultPollingInterval = TimeUnit.SECONDS.toMillis(1);
    public Long readTimeout;
    public Long writeTimeout;
    public String token;

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
        XInfiniConnectConfig config = new XInfiniConnectConfig();

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

        public Builder setReadTimeout(long value, TimeUnit unit) {
            config.readTimeout = unit.toMillis(value);
            return this;
        }

        public Builder setWriteTimeout(long value, TimeUnit unit) {
            config.writeTimeout = unit.toMillis(value);
            return this;
        }


        public XInfiniConnectConfig build() {
            return config;
        }
    }
}
