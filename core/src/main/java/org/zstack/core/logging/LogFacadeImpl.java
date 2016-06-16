package org.zstack.core.logging;

import org.zstack.core.Platform;

/**
 * Created by xing5 on 2016/6/14.
 */
public class LogFacadeImpl implements LogFacade {
    private LogBackend backend;

    @Override
    public LogBackend getBackend() {
        if (backend == null) {
            backend = Platform.getComponentLoader().getComponent(LogGlobalProperty.LOGGING_BACKEND);
        }
        return backend;
    }
}
