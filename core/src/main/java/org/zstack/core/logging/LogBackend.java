package org.zstack.core.logging;

/**
 */
public interface LogBackend {
    void write(LogVO log);

    String getLogBackendType();

    void start();

    void stop();
}
