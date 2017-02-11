package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.test.BeanConstructor;

import java.util.concurrent.TimeUnit;

/**
 */
public class TestAsyncBackup7 {
    ComponentLoader loader;
    ThreadFacade thdf;
    String syncName = "test";
    boolean success1 = false;
    boolean success2 = false;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        thdf = loader.getComponent(ThreadFacade.class);
    }

    @Test
    public void test() throws InterruptedException {
        thdf.chainSubmit(new ChainTask(new NoErrorCompletion() {
            @Override
            public void done() {
                success1 = true;
            }
        }) {
            @Override
            public String getSyncSignature() {
                return syncName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                throw new RuntimeException("on purpose");
            }

            @Override
            public String getName() {
                return syncName;
            }
        });
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return syncName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                success2 = true;
                chain.next();
            }

            @Override
            public String getName() {
                return syncName;
            }
        });

        TimeUnit.SECONDS.sleep(2);
        Assert.assertTrue(success1);
        Assert.assertTrue(success2);
    }

}
