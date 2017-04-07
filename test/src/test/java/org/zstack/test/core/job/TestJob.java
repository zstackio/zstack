package org.zstack.test.core.job;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.test.Api;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestJob {
    Api api;
    ComponentLoader loader;
    JobQueueFacade jobf;
    long num = 50;
    FakeJobConfig fl;
    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        loader = con.addXml("JobForUnitTest.xml").addXml("PortalForUnitTest.xml").addXml("AccountManager.xml").build();
        jobf = loader.getComponent(JobQueueFacade.class);
        fl = loader.getComponent(FakeJobConfig.class);
        api = new Api();
        api.startServer();
    }

    private void startJob(long index) {
        FakeJob fjob = new FakeJob(index);
        jobf.execute("fake-job", "TestJob", fjob);
    }

    @Test
    public void test() throws InterruptedException {
        for (long i = 0; i < num; i++) {
            startJob(i);
        }

        TimeUnit.SECONDS.sleep(15);
        long count = 0;
        for (Long l : fl.indexs) {
            if (l < count) {
                Assert.fail();
            }
            count = l;
        }
    }
}
