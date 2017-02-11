package org.zstack.test.multinodes;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusEventListener;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.Event;
import org.zstack.header.message.MessageReply;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * start 2 nodes, send silent message to the node0 which won't reply
 * stop node0, we should receive error message which indicates node0 is unavailable
 */
public class TestMultipleNode5 {
    CLogger logger = Utils.getLogger(TestMultipleNode5.class);
    ComponentLoader loader;
    NodeManager nodeMgr;
    CloudBus bus;
    Api api;
    volatile boolean success1 = false;
    volatile boolean success2 = false;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        con.addXml("PortalForUnitTest.xml");
        con.addXml("AccountManager.xml");
        // have to load silentService, otherwise message tracker won't care it
        con.addXml("silentService.xml");
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
        nodeMgr = new NodeManager();
        api = new Api();
        api.startServer();
        api.setTimeout(300);
    }


    @Test
    public void test() throws InterruptedException, ApiSenderException {
        try {
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

            NeedReplySilentMsg smsg = new NeedReplySilentMsg();
            bus.makeServiceIdByManagementNodeId(smsg, SilentService.SERVICE_ID, target.getUuid());
            bus.send(smsg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess() && reply.getError().getCode().equals(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR.toString())) {
                        logger.debug("received MANAGEMENT_NODE_UNAVAILABLE_ERROR");
                        success1 = true;
                    }
                }
            });

            final APISilentMsg amsg = new APISilentMsg();
            bus.makeServiceIdByManagementNodeId(amsg, SilentService.SERVICE_ID, target.getUuid());
            amsg.setSession(api.getAdminSession());
            bus.subscribeEvent(new CloudBusEventListener() {
                @Override
                public boolean handleEvent(Event e) {
                    if (e instanceof APIEvent) {
                        APIEvent evt = (APIEvent) e;
                        if (evt.getApiId().equals(amsg.getId()) &&
                                !evt.isSuccess() && evt.getError().getCode().equals(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR.toString())) {
                            success2 = true;
                        }
                    }
                    return false;
                }
            }, new APIEvent());

            bus.send(amsg);

            nodeMgr.stopNode(0, 120);

            TimeUnit.SECONDS.sleep(5);
            Assert.assertTrue(success1);
            Assert.assertTrue(success2);
        } finally {
            nodeMgr.stopNodes(120);
        }
    }
}
