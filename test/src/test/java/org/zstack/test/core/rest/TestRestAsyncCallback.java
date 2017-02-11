package org.zstack.test.core.rest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.test.WebBeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestRestAsyncCallback {
    CLogger logger = Utils.getLogger(TestRestAsyncCallback.class);
    WebBeanConstructor wbean;
    ComponentLoader loader;
    RESTFacade restf;
    String url;
    CountDownLatch latch = new CountDownLatch(1);
    boolean success = false;

    @Before
    public void setUp() throws Exception {
        wbean = new WebBeanConstructor();
        wbean.addXml("PortalForUnitTest.xml").addXml("AccountManager.xml");
        loader = wbean.build();
        restf = loader.getComponent(RESTFacade.class);
        url = wbean.getSiteUrl();
    }

    @Test
    public void test() throws InterruptedException {
        String url = wbean.buildUrl(RESTBeanForTest.ROOT, RESTBeanForTest.CALLBACK_PATH);
        final String hi = "hello";
        restf.asyncJsonPost(url, hi, new JsonAsyncRESTCallback<String>(null) {

            @Override
            public void fail(ErrorCode err) {
                logger.error(String.format("fail: %s", err));
                success = false;
                latch.countDown();
            }

            @Override
            public void success(String ret) {
                if (hi.equals(ret)) {
                    success = true;
                } else {
                    logger.error(String.format("expected: %s, but got: %s", hi, ret));
                    success = false;
                }
                latch.countDown();
            }

            @Override
            public Class<String> getReturnClass() {
                return String.class;
            }

        }, TimeUnit.SECONDS, 10);
        latch.await(1, TimeUnit.MINUTES);
        Assert.assertTrue(success);
    }

    @After
    public void tearDown() {
        wbean.stopJetty();
    }

}
