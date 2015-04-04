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

import java.util.concurrent.TimeUnit;

public class TestJobReturnValueFail {
    CLogger logger = Utils.getLogger(TestJobReturnValueFail.class);
    ComponentLoader loader;
    JobQueueFacade jobf;
    long num = 10;
    boolean success = true;
    int retGot = 0;
    
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
        FakeJobReturnValueFail fjob = new FakeJobReturnValueFail(index);
        jobf.execute("fake-job", "TestJob", fjob, new ReturnValueCompletion<Long>() {
            @Override
            public void success(Long returnValue) {
                logger.debug(String.format("job[%s] unwanted success", returnValue));
                success = false;
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(errorCode.toString());
                retGot ++;
            }
        }, Long.class);
    }
    
    @Test
    public void test() throws InterruptedException {
        for (long i=0; i<num; i++) {
            startJob(i);
        }
        
        TimeUnit.SECONDS.sleep(15);
        Assert.assertTrue(success);
        Assert.assertEquals(num, retGot);
    }
}
