package org.zstack.test.multinodes;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.core.config.GlobalConfigForTest;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. start 2 nodes
 * 2. update global config test
 * 3. instruct node1 to report global config test
 * <p>
 * confirm global config was updated on node1, but other global configs don't change
 */
public class TestMultipleNode13 {
    CLogger logger = Utils.getLogger(TestMultipleNode13.class);
    ComponentLoader loader;
    NodeManager nodeMgr;
    CloudBusIN bus;
    EventFacade evtf;
    Api api;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        con.addXml("PortalForUnitTest.xml");
        con.addXml("AccountManager.xml");
        con.addXml("silentService.xml");
        loader = con.build();
        bus = loader.getComponent(CloudBusIN.class);
        nodeMgr = new NodeManager();
        evtf = loader.getComponent(EventFacade.class);
        api = new Api();
        api.startServer();
        api.setTimeout(300);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        nodeMgr.setProperties("-DbFacadeDataSource.maxPoolSize=50")
                .createLightWeightNodeWithCore(1, 300, Arrays.asList("silentService.xml"));
        List<ManagementNodeInventory> nodes = api.listManagementNodes();
        ManagementNodeInventory target = CollectionUtils.find(nodes, new Function<ManagementNodeInventory, ManagementNodeInventory>() {
            @Override
            public ManagementNodeInventory call(final ManagementNodeInventory arg) {
                if (!arg.getUuid().equals(Platform.getManagementServerId())) {
                    return arg;
                }
                return null;
            }
        });

        String old = GlobalConfigForTest.TEST2.value();
        GlobalConfigForTest.TEST.updateValue(10000);
        TimeUnit.SECONDS.sleep(5);
        try {
            ReportGlobalConfigMsg msg = new ReportGlobalConfigMsg();
            bus.makeServiceIdByManagementNodeId(msg, ReportGlobalConfigService.SERVICE_ID, target.getUuid());
            ReportGlobalConfigReply reply = (ReportGlobalConfigReply) bus.call(msg);
            Assert.assertEquals("10000", reply.getValue());
            Assert.assertEquals(old, reply.getValue2());
        } finally {
            nodeMgr.stopNodes(120);
        }
    }
}
