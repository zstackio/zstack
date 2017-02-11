package org.zstack.test.core.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.test.WebBeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestRestAsyncCallback1 {
    CLogger logger = Utils.getLogger(TestRestAsyncCallback1.class);
    WebBeanConstructor wbean;
    ComponentLoader loader;
    RESTFacade restf;
    String url;
    int num = 200;
    CountDownLatch latch = new CountDownLatch(num);
    volatile List<Boolean> success = new ArrayList<Boolean>(num);

    @Before
    public void setUp() throws Exception {
        wbean = new WebBeanConstructor();
        wbean.addXml("PortalForUnitTest.xml").addXml("AccountManager.xml");
        loader = wbean.build();
        restf = loader.getComponent(RESTFacade.class);
        url = wbean.getSiteUrl();
    }

    @AsyncThread
    private void call() {
        final String url = wbean.buildUrl(RESTBeanForTest.ROOT, RESTBeanForTest.CALLBACK_PATH);
        final String hi = Platform.getUuid();
        final TestRestAsyncCallback1 self = this;
        restf.asyncJsonPost(url, hi, new JsonAsyncRESTCallback<String>(null) {

            @Override
            public void fail(ErrorCode err) {
                synchronized (self) {
                    logger.error(String.format("fail: %s", err));
                    success.add(false);
                    latch.countDown();
                }
            }

            @Override
            public void success(String ret) {
                synchronized (self) {
                    if (hi.equals(ret)) {
                        success.add(true);
                    } else {
                        logger.error(String.format("expected: %s, but got: %s", hi, ret));
                        success.add(false);
                    }
                    latch.countDown();
                }
            }

            @Override
            public Class<String> getReturnClass() {
                return String.class;
            }

        }, TimeUnit.SECONDS, 5);
    }

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < num; i++) {
            call();
        }

        latch.await(2, TimeUnit.MINUTES);
        Assert.assertEquals(num, success.size());
        for (Boolean r : success) {
            Assert.assertTrue(r);
        }
    }

}
