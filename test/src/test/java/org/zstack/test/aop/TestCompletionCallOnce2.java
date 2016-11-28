package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 */
public class TestCompletionCallOnce2 {
    CLogger logger = Utils.getLogger(TestCompletionCallOnce2.class);
    int success = 0;
    ComponentLoader loader;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
    }

    @Test
    public void test() throws InterruptedException {
        NoErrorCompletion completion = new NoErrorCompletion() {
            @Override
            public void done() {
                success++;
            }
        };

        completion.done();
        completion.done();

        Assert.assertEquals(1, success);
    }

}
