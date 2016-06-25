package org.zstack.core.aspect;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public aspect ExceptionSafeAspect {
    private static final CLogger logger = Utils.getLogger(ExceptionSafeAspect.class);

    void around() : execution(@org.zstack.header.core.ExceptionSafe * *.*(..)) {
        try {
            proceed();
        } catch (Throwable t) {
            logger.warn("unhandled exception happened", t);
        }
    }
}
