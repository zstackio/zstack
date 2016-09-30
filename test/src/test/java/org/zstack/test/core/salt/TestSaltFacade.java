package org.zstack.test.core.salt;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.salt.SaltFacadeImpl;
import org.zstack.test.BeanConstructor;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 */
public class TestSaltFacade {
    ComponentLoader loader;
    SaltFacadeImpl saltf;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        saltf = loader.getComponent(SaltFacadeImpl.class);
    }

    @Test
    public void test() throws InterruptedException, IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        saltf.start();
        saltf.deployModule("salt/kvm");
    }
}
