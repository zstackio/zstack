package org.zstack.test.core.defer;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestDefer2 {
    CLogger logger = Utils.getLogger(TestDefer2.class);
    int count = 0;
    boolean ret1 = false;
    boolean ret2 = false;
    boolean ret3 = false;
    boolean ret4 = false;
    boolean ret5 = false;

    @Before
    public void setUp() throws Exception {
    }


    @Deferred
    private void case2() {
        ret2 = true;
    }

    @Deferred
    private void case3() {
        ret3 = true;
        Defer.guard(new Runnable() {
            @Override
            public void run() {
                ret3 = false;
            }
        });
    }

    @Deferred
    private void case4() {
        ret4 = true;
        case5();
    }

    @Deferred
    private void case5() {
        ret5 = true;
        Defer.guard(new Runnable() {
            @Override
            public void run() {
                ret5 = false;
            }
        });
        throw new CloudRuntimeException("Roll back count");
    }

    @Deferred
    private void case1() {
        ret1 = true;
        Defer.guard(new Runnable() {
            @Override
            public void run() {
                ret1 = false;
            }
        });
        case2();
        case3();
        case4();
    }

    @Test(expected = CloudRuntimeException.class)
    public void test() {
        case1();
        Assert.assertFalse(ret1);
        Assert.assertTrue(ret2);
        Assert.assertTrue(ret3);
        Assert.assertTrue(ret4);
        Assert.assertFalse(ret5);
    }
}
