package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ThreadGlobalProperty;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.APIGetIpAddressCapacityReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 1. attach 50 nic to vm concurrently
 * 2. detach the 50 nic concurrently
 * <p>
 * confirm all nics attached/detached successfully
 */
public class TestDetachNicOnKvm8 {
    CLogger logger = Utils.getLogger(TestDetachNicOnKvm8.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        ThreadGlobalProperty.MAX_THREAD_NUM = 500;
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network4");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        APIGetIpAddressCapacityReply ipcap = api.getIpAddressCapacityByAll();
        long avail1 = ipcap.getAvailableCapacity();

        int num = 50;
        final CountDownLatch latch = new CountDownLatch(num);
        final String vmUuid = vm.getUuid();

        class Ret {
            int count;
        }

        final Ret ret = new Ret();
        for (int i = 0; i < num; i++) {
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    try {
                        VmInstanceInventory v = api.attachNic(vmUuid, l3.getUuid());
                        ret.count += 1;
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e);
                    } finally {
                        latch.countDown();
                    }
                }
            }.run();
        }

        latch.await(120, TimeUnit.SECONDS);

        VmInstanceVO vmvo = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        final CountDownLatch latch1 = new CountDownLatch(vmvo.getVmNics().size());
        for (final VmNicVO nic : vmvo.getVmNics()) {
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    try {
                        api.detachNic(nic.getUuid());
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e);
                    } finally {
                        latch1.countDown();
                    }
                }
            }.run();
        }

        latch1.await(120, TimeUnit.SECONDS);

        TimeUnit.SECONDS.sleep(8);
        ipcap = api.getIpAddressCapacityByAll();
        long avail2 = ipcap.getAvailableCapacity();

        Assert.assertEquals(avail1, avail2 - 3);
    }
}
