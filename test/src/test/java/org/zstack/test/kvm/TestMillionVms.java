package org.zstack.test.kvm;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TestMillionVms {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;
    int num = 100;
    CountDownLatch latch = new CountDownLatch(num);
    String iouuid;
    String imuuid;
    List<String> l3uuids;
    List<String> ds;
    VmInstanceInventory vm = new VmInstanceInventory();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm100.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @AsyncThread
    void createVm() throws ApiSenderException {
        for (int i = 0; i < 1000; i++) {
            api.createVmByFullConfig(vm, null, l3uuids, ds);
        }
        latch.countDown();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ImageInventory im = deployer.images.get("TestImage");
        imuuid = im.getUuid();
        InstanceOfferingInventory io = deployer.instanceOfferings.get("TestInstanceOffering");
        iouuid = io.getUuid();
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        l3uuids = new ArrayList<String>(1);
        l3uuids.add(l3.getUuid());
        ds = new ArrayList<String>(0);
        vm = new VmInstanceInventory();
        vm.setDescription("TestVm");
        vm.setName("TestVm");
        vm.setType(VmInstanceConstant.USER_VM_TYPE);
        vm.setInstanceOfferingUuid(iouuid);
        vm.setImageUuid(imuuid);
        for (int i = 0; i < num; i++) {
            createVm();
        }
        latch.await();
    }

}
