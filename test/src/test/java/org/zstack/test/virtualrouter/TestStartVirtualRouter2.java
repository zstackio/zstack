package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.appliancevm.ApplianceVmVO_;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TestStartVirtualRouter2 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;
    int num = 5;
    CountDownLatch latch = new CountDownLatch(num);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/startVirtualRouter.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @AsyncThread
    private void createVm() throws ApiSenderException {
        try {
            ImageInventory iminv = deployer.images.get("TestImage");
            InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
            L3NetworkInventory l3inv = deployer.l3Networks.get("TestL3Network2");
            APICreateVmInstanceMsg msg = new APICreateVmInstanceMsg();
            msg.setImageUuid(iminv.getUuid());
            msg.setInstanceOfferingUuid(ioinv.getUuid());
            List<String> l3uuids = new ArrayList<String>();
            l3uuids.add(l3inv.getUuid());
            msg.setL3NetworkUuids(l3uuids);
            msg.setName("TestVm");
            msg.setSession(session);
            msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
            msg.setType(VmInstanceConstant.USER_VM_TYPE);
            ApiSender sender = api.getApiSender();
            sender.send(msg, APICreateVmInstanceEvent.class);
        } finally {
            latch.countDown();
        }
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        for (int i = 0; i < num; i++) {
            createVm();
        }
        latch.await();

        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.add(VmInstanceVO_.type, Op.EQ, VmInstanceConstant.USER_VM_TYPE);
        q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Running);
        long count = q.count();
        Assert.assertEquals(num, count);

        SimpleQuery<ApplianceVmVO> aq = dbf.createQuery(ApplianceVmVO.class);
        aq.add(ApplianceVmVO_.type, Op.EQ, ApplianceVmConstant.APPLIANCE_VM_TYPE);
        aq.add(ApplianceVmVO_.state, Op.EQ, VmInstanceState.Running);
        count = aq.count();
        Assert.assertEquals(1, count);
    }
}
