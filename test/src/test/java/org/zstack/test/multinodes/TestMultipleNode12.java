package org.zstack.test.multinodes;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.AbstractService;
import org.zstack.header.Service;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.LockResourceMessage;
import org.zstack.header.message.LockResourceReply;
import org.zstack.header.message.Message;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.multinodes.SilentLockService.DoLockMessage;
import org.zstack.test.multinodes.SilentLockService.SilentLockResourceMsg;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 1. start 2 nodes
 * 2. instruct node0 to send lock message to node1
 * 3. stop node0
 * <p>
 * confirm node1 receives unlock message
 */
public class TestMultipleNode12 {
    CLogger logger = Utils.getLogger(TestMultipleNode12.class);
    ComponentLoader loader;
    NodeManager nodeMgr;
    CloudBusIN bus;
    EventFacade evtf;
    Api api;
    volatile boolean locked = false;
    volatile boolean unlocked = false;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        con.addXml("PortalForUnitTest.xml");
        con.addXml("AccountManager.xml");
        // have to load silentService, otherwise message tracker won't care it
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
        try {
            final String SERVICE_ID = "toLockService";

            Service serv = new AbstractService() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg instanceof SilentLockResourceMsg) {
                        final SilentLockResourceMsg dmsg = (SilentLockResourceMsg) msg;
                        locked = true;
                        evtf.on(LockResourceMessage.UNLOCK_CANONICAL_EVENT_PATH, new AutoOffEventCallback() {
                            @Override
                            public boolean run(Map tokens, Object data) {
                                if (dmsg.getUnlockKey().equals(data)) {
                                    unlocked = true;
                                    return true;
                                }
                                return false;
                            }
                        });

                        LockResourceReply r = new LockResourceReply();
                        bus.reply(msg, r);
                    }
                }

                @Override
                public String getId() {
                    return bus.makeLocalServiceId(SERVICE_ID);
                }

                @Override
                public boolean start() {
                    return true;
                }

                @Override
                public boolean stop() {
                    return true;
                }
            };

            bus.registerService(serv);
            bus.activeService(serv);

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

            DoLockMessage dlmsg = new DoLockMessage();
            dlmsg.toLockServiceId = SERVICE_ID;
            dlmsg.toManagementNodeUuid = Platform.getManagementServerId();
            bus.makeServiceIdByManagementNodeId(dlmsg, SilentLockService.SERVICE_ID, target.getUuid());
            bus.call(dlmsg);

            nodeMgr.stopNode(0, 120);

            TimeUnit.SECONDS.sleep(5);
            Assert.assertTrue(locked);
            Assert.assertTrue(unlocked);
        } finally {
            nodeMgr.stopNodes(120);
        }
    }
}
