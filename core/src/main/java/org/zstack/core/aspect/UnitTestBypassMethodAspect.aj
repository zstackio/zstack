package org.zstack.core.aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 */
public aspect UnitTestBypassMethodAspect {
    private static final CLogger logger = Utils.getLogger(UnitTestBypassMethodAspect.class);

    Object around() : execution(@org.zstack.header.core.BypassWhenUnitTest * *.*(..)) {
        logger.debug(String.format("bypass %s because of unit test", thisJoinPoint.getSignature().toLongString()));
        return null;
    }
}
