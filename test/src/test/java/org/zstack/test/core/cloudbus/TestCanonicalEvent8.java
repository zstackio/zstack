package org.zstack.test.core.cloudbus;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 12:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestCanonicalEvent8 {
    CLogger logger = Utils.getLogger(TestCanonicalEvent8.class);
    ComponentLoader loader;
    EventFacade evtf;
    int count;

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
        evtf.on(path, new AutoOffEventCallback() {
            @Override
            public boolean run(Map tokens, Object data) {
                count++;
                return true;
            }
        });

        evtf.fire(path, null);
        evtf.fire(path, null);
        evtf.fire(path, null);
        TimeUnit.SECONDS.sleep(2);
        Assert.assertEquals(1, count);
    }
}

