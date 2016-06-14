package org.zstack.test.core.gc;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBusImpl2;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.gc.GCFacade;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.test.UnitTestUtils;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * test event based GC
 */
public class TestGC10 {
    ComponentLoader loader;
    GCFacade gcf;
    Deployer deployer;
    DatabaseFacade dbf;
    EventFacadeImpl evtf;
    CloudBusImpl2 bus;
    static boolean success;

    @Before
    public void setUp() throws Exception {
        UnitTestUtils.runTestCase(TestGC9PreCase.class);
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/OnlyOneZone.xml", con);
        deployer.load();
        loader = deployer.getComponentLoader();
        gcf = loader.getComponent(GCFacade.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        evtf = loader.getComponent(EventFacadeImpl.class);
        bus = loader.getComponent(CloudBusImpl2.class);
        deployer.build();
    }


    @Test
    public void test() throws InterruptedException {
        String eventPath = "/test/gc";
        evtf.fire(eventPath, "hello");
        TimeUnit.SECONDS.sleep(3);
        List<GarbageCollectorVO> vos = dbf.listAll(GarbageCollectorVO.class);
        for (GarbageCollectorVO vo : vos) {
            Assert.assertEquals(GCStatus.Done, vo.getStatus());
        }
    }
}
