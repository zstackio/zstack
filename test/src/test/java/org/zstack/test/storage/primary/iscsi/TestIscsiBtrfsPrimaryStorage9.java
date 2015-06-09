package org.zstack.test.storage.primary.iscsi;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiBtrfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiIsoVO;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 1. create VM1 using ISO with IscsiBtrfsPrimaryStorage
 * 2. stop VM1
 * 2. concurrently create 100 VMs
 *
 * confirm 100 ISO in iscsi ISO store
 *
 */
public class TestIscsiBtrfsPrimaryStorage9 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    KVMSimulatorConfig kconfig;
    IscsiBtrfsPrimaryStorageSimulatorConfig iconfig;
    int vmNum = 100;
    CountDownLatch latch = new CountDownLatch(100);

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
        final ImageInventory iso = deployer.images.get("TestImage");
        final DiskOfferingInventory root = deployer.diskOfferings.get("TestRootDiskOffering");
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        final InstanceOfferingInventory ins = deployer.instanceOfferings.get("TestInstanceOffering");
        api.stopVmInstance(vm.getUuid());

        for (int i = 0; i < vmNum; i++) {
            final int finalI = i;
            Runnable runnable = new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    VmCreator creator = new VmCreator(api);
                    creator.name = "TestVm" + finalI;
                    creator.imageUuid = iso.getUuid();
                    creator.rootDiskOfferingUuid = root.getUuid();
                    creator.addL3Network(l3.getUuid());
                    creator.instanceOfferingUuid = ins.getUuid();
                    try {
                        VmInstanceInventory vm2 = creator.create();
                    } catch (ApiSenderException e) {
                        throw new CloudRuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            };

            runnable.run();
        }

        latch.await(60, TimeUnit.SECONDS);
        long count = dbf.count(IscsiIsoVO.class);
        Assert.assertEquals(100, count);
    }
}
