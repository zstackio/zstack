package org.zstack.test.aop;

import org.junit.Test;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 */
public class TestExceptionSafe {
    CLogger logger = Utils.getLogger(TestExceptionSafe.class);


    @ExceptionSafe
    private void testMethod(int unusedParam) {
        throw new RuntimeException("on purpose");
    }

    @Test
    public void test() throws InterruptedException {
        testMethod(1);
    }
}
