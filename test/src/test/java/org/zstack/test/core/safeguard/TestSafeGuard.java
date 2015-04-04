package org.zstack.test.core.safeguard;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.safeguard.Guard;
import org.zstack.core.safeguard.SafeGuard;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestSafeGuard {
    CLogger logger = Utils.getLogger(TestSafeGuard.class);
    int count = 0;

    @Before
    public void setUp() throws Exception {
    }

    
    @Guard
    private void case1() {
        count ++;
        SafeGuard.guard(new Runnable() {
            @Override
            public void run() {
                count --;
            }
        });
        throw new CloudRuntimeException("Roll back count");
    }
    
    @Test(expected=CloudRuntimeException.class)
    public void test() {
        case1();
        Assert.assertEquals(0, count);
    }
}
