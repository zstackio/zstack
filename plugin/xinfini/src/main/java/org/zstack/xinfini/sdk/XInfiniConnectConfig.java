package org.zstack.xinfini.sdk;

import org.zstack.xinfini.XInfiniConfig;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class XInfiniConnectConfig {
    public String hostname = "localhost";
    public int port = 443;
    long defaultPollingTimeout = TimeUnit.HOURS.toMillis(3);
    long defaultPollingInterval = TimeUnit.SECONDS.toMillis(1);
    public Long readTimeout;
    public Long writeTimeout;
    public String token;
    public Supplier<XInfiniConfig.Node> connectedNodeProvider;

    public void setPort(int port) {
        this.port = port;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

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
}
