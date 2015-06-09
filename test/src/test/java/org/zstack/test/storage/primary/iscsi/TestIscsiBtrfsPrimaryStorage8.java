package org.zstack.test.storage.primary.iscsi;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOffering;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiBtrfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiIsoVO;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;

/**
 * 1. create VM using ISO with IscsiBtrfsPrimaryStorage
 * 2. stop VM
 * 3. create another VM
 *
 * confirm the old ISO in iscsi ISO store is reused
 *
 */
public class TestIscsiBtrfsPrimaryStorage8 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    KVMSimulatorConfig kconfig;
    IscsiBtrfsPrimaryStorageSimulatorConfig iconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/iscsiBtrfsPrimaryStorage/TestIscsiBtrfsPrimaryStorage1.xml", con);
        deployer.addSpringConfig("iscsiBtrfsPrimaryStorage.xml");
        deployer.addSpringConfig("iscsiFileSystemPrimaryStorageSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        iconfig = loader.getComponent(IscsiBtrfsPrimaryStorageSimulatorConfig.class);
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        ImageInventory iso = deployer.images.get("TestImage");
        DiskOfferingInventory root = deployer.diskOfferings.get("TestRootDiskOffering");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        InstanceOfferingInventory ins = deployer.instanceOfferings.get("TestInstanceOffering");
        api.stopVmInstance(vm.getUuid());

        VmCreator creator = new VmCreator(api);
        creator.name = "TestVm2";
        creator.imageUuid = iso.getUuid();
        creator.rootDiskOfferingUuid = root.getUuid();
        creator.addL3Network(l3.getUuid());
        creator.instanceOfferingUuid = ins.getUuid();
        VmInstanceInventory vm2 = creator.create();

        long count = dbf.count(IscsiIsoVO.class);
        Assert.assertEquals(1, count);
        IscsiIsoVO vo = dbf.listAll(IscsiIsoVO.class).get(0);
        Assert.assertEquals(vm2.getUuid(), vo.getVmInstanceUuid());
    }
}
