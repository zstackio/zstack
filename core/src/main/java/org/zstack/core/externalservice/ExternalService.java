package org.zstack.core.externalservice;

public interface ExternalService {
    String getName();

    void start();

    void stop();

    void restart();

    boolean isAlive();
}
