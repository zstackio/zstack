package org.zstack.test.core.defer;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestDefer1 {
    CLogger logger = Utils.getLogger(TestDefer1.class);
    int count = 0;

    @Before
    public void setUp() throws Exception {
    }


    @Deferred
    private void case1() {
        count++;
        Defer.guard(new Runnable() {
            @Override
            public void run() {
                count--;
            }
        });
        throw new CloudRuntimeException("Roll back count");
    }

    @Test(expected = CloudRuntimeException.class)
    public void test() {
        case1();
        Assert.assertEquals(0, count);
    }
}
