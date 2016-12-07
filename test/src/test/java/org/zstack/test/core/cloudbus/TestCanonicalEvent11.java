package org.zstack.test.core.cloudbus;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 12:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestCanonicalEvent11 {
    CLogger logger = Utils.getLogger(TestCanonicalEvent11.class);
    ComponentLoader loader;
    EventFacade evtf;
    AtomicInteger count = new AtomicInteger(0);

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        evtf = loader.getComponent(EventFacade.class);
        ((EventFacadeImpl) evtf).start();
    }

    @Test
    public void test() throws InterruptedException {
        String path = "/test/event";
        EventCallback cb = new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                count.incrementAndGet();
            }
        };

        EventCallback cb1 = new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                count.incrementAndGet();
            }
        };

        evtf.on(path, cb);
        evtf.on(path, cb1);

        evtf.fire(path, null);
        TimeUnit.SECONDS.sleep(1);

        Assert.assertEquals(2, count.intValue());
    }
}

