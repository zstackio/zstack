package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.timeout.ApiTimeout;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Test API timeout
 */
public class TestLocalStorage46 {
    CLogger logger = Utils.getLogger(TestLocalStorage46.class);

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    RESTFacade restf;
    ApiTimeoutManager timeoutManager;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage32.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);
        restf = loader.getComponent(RESTFacade.class);
        timeoutManager = loader.getComponent(ApiTimeoutManager.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());
        VolumeInventory data = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                return VolumeType.Data.toString().equals(arg.getType()) ? arg : null;
            }
        });
        api.detachVolumeFromVm(data.getUuid());

        VolumeInventory root = vm.getRootVolume();
        HostInventory host2 = deployer.hosts.get("host2");

        final List<Message> msgs = new ArrayList<Message>();
        final Map<String, Long> commands = new HashMap<String, Long>();
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                msgs.add(msg);
            }
        });
        restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            public void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
                commands.put(body.getClass().getName(), TimeUnit.MILLISECONDS.convert(timeout, unit));
            }

            @Override
            public void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
            }
        });

        api.localStorageMigrateVolume(root.getUuid(), host2.getUuid(), null);

        ApiTimeout at = timeoutManager.getAllTimeout().get(APILocalStorageMigrateVolumeMsg.class);

        List<String> relativeNames = CollectionUtils.transformToList(at.getRelatives(), new Function<String, Class>() {
            @Override
            public String call(Class arg) {
                return arg.getName();
            }
        });

        List<String> tested = new ArrayList<String>();

        for (Message msg : msgs) {
            if (msg instanceof KVMHostAsyncHttpCallMsg) {
                final KVMHostAsyncHttpCallMsg kmsg = (KVMHostAsyncHttpCallMsg) msg;
                if (relativeNames.contains(kmsg.getCommandClassName())) {
                    long commandTimeout = kmsg.getCommandTimeout();
                    Assert.assertEquals(String.format("timeout mismatch for %s, expected: %s, actual: %s", kmsg.getCommandClassName(),
                            at.getTimeout(), commandTimeout), at.getTimeout(), commandTimeout);
                    tested.add(kmsg.getCommandClassName());
                }
            }
        }

        for (final Class clz : at.getRelatives()) {
            Message msg = CollectionUtils.find(msgs, new Function<Message, Message>() {
                @Override
                public Message call(Message arg) {
                    return arg.getClass() == clz ? arg : null;
                }
            });
            Long commandTimeout = commands.get(clz.getName());

            if (msg != null && msg instanceof NeedReplyMessage) {
                NeedReplyMessage nmsg = (NeedReplyMessage) msg;
                Assert.assertEquals(String.format("timeout mismatch for %s, expected: %s, actual: %s", msg.getClass().getName(),
                        at.getTimeout(), nmsg.getTimeout()), at.getTimeout(), nmsg.getTimeout());
            } else if (commandTimeout != null) {
                Assert.assertEquals(String.format("timeout mismatch for %s, expected: %s, actual: %s", clz.getName(),
                        at.getTimeout(), commandTimeout), at.getTimeout(), commandTimeout.longValue());
            } else {
                if (!tested.contains(clz.getName())) {
                    logger.warn(String.format("API relative[%s] is not captured", clz.getName()));
                }
            }
        }
    }
}
