package org.zstack.test;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;

/**
 */
public class TestString {
    CLogger logger = Utils.getLogger(TestString.class);

    @Test
    public void test() throws InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ThreadContext.put("api", Platform.getUuid());
        logger.debug("message 1");
        logger.info("message 2");
        logger.warn("message 3");
        logger.error("message 4");
        logger.trace("message 5");
        ThreadContext.clearAll();
    }
}
