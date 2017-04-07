package org.zstack.test.core.job;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.test.Api;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;

import java.util.concurrent.TimeUnit;

public class TestJob2 {
    ComponentLoader loader;
    JobQueueFacade jobf;
    long num = 10;
    FakeJobConfig fl;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        loader = con.addXml("JobForUnitTest.xml").addXml("PortalForUnitTest.xml").addXml("AccountManager.xml").build();
        jobf = loader.getComponent(JobQueueFacade.class);
        fl = loader.getComponent(FakeJobConfig.class);
        new Api().startServer();
    }

    @AsyncThread
    private void startJob() {
        FakeJob2 fjob = new FakeJob2();
        jobf.execute("fake-job", "TestJob", fjob);
    }

    @Test
    public void test() throws InterruptedException {
        fl.success = true;
        for (long i = 0; i < num; i++) {
            startJob();
        }

        TimeUnit.SECONDS.sleep(15);
        Assert.assertTrue(fl.success);
    }
}
