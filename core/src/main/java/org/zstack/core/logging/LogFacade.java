package org.zstack.core.logging;

/**
 */
public interface LogFacade {
    boolean isEnabled();

    void info(String resourceUuid, String info);

    void warn(String resourceUuid, String info);

    void error(String resourceUuid, String info);
}
