package org.zstack.test.core.job;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.Api;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestJobReturnValue {
    CLogger logger = Utils.getLogger(TestJobReturnValue.class);
    ComponentLoader loader;
    JobQueueFacade jobf;
    int num = 1000;
    boolean success = true;
    int retGot = 0;
    CountDownLatch latch = new CountDownLatch(num);
    
    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        loader = con.addXml("JobForUnitTest.xml").addXml("PortalForUnitTest.xml").addXml("AccountManager.xml").build();
        jobf = loader.getComponent(JobQueueFacade.class);
        new Api().startServer();
    }

    @AsyncThread
    private void startJob(final long index) {
        FakeJobReturnValue fjob = new FakeJobReturnValue(index);
        jobf.execute("fake-job", "TestJob", fjob, new ReturnValueCompletion<Long>() {
            @Override
            public void success(Long returnValue) {
                logger.debug(String.format("get return value[%s]", returnValue));
                retGot ++;
                if (returnValue != index) {
                    logger.debug(String.format("expect %s but %s", index, returnValue));
                    success = false;
                }
                latch.countDown();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(errorCode.toString());
                success = false;
                latch.countDown();
            }
        }, Long.class);
    }
    
    @Test
    public void test() throws InterruptedException {
        for (long i=0; i<num; i++) {
            startJob(i);
        }

        latch.await(240, TimeUnit.SECONDS);
        Assert.assertTrue(success);
        Assert.assertEquals(num, retGot);
    }
}
