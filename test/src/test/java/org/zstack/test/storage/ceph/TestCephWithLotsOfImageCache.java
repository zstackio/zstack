package org.zstack.test.storage.ceph;

import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;

public class TestCephWithLotsOfImageCache {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;
    CephBackupStorageSimulatorConfig bconfig;
    RESTFacade restf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCephWithLotsOfImageCache.xml", con);
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bconfig = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        restf = loader.getComponent(RESTFacade.class);
        session = api.loginAsAdmin();
    }

    public void createVm(String imageNo) throws ApiSenderException {
        List<String> l3uuids;
        List<String> ds;
        VmInstanceInventory vm = new VmInstanceInventory();
        String iouuid;
        String imuuid;

        ImageInventory im = deployer.images.get("TestImage" + imageNo);
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

        api.createVmByFullConfig(vm, null, l3uuids, ds);
    }

    @Test
    public void test() throws ApiSenderException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        PrimaryStorageInventory ps = deployer.primaryStorages.get("ceph-pri");
        BackupStorageInventory bs = deployer.backupStorages.get("ceph-bk");

        createVm("1");
        createVm("2");
        createVm("3");
        createVm("4");
        createVm("5");
        createVm("6");
    }
}
