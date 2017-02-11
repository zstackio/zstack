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
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestRestAsyncCallback2 {
    CLogger logger = Utils.getLogger(TestRestAsyncCallback2.class);
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
        String url = wbean.buildUrl(RESTBeanForTest.ROOT, RESTBeanForTest.CALLBACK_JSON_PATH);
        final Map<String, String> hi = new HashMap<String, String>();
        hi.put("hello", "world");
        restf.asyncJsonPost(url, hi, new JsonAsyncRESTCallback<Map>(null) {

            @Override
            public void fail(ErrorCode err) {
                logger.error(String.format("fail: %s", err));
                success = false;
                latch.countDown();
            }

            @Override
            public void success(Map ret) {
                if (ret.get("hello").equals("world")) {
                    success = true;
                } else {
                    logger.error(String.format("expected: %s, but got: %s", JSONObjectUtil.toJsonString(hi), JSONObjectUtil.toJsonString(ret)));
                    success = false;
                }
                latch.countDown();
            }

            @Override
            public Class<Map> getReturnClass() {
                return Map.class;
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
