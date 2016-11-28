package org.zstack.test.compute.vm;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.SyncThread;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// this is called by other unit test cases
public class Create100Vm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    int vmNum = 100;
    CountDownLatch latch = new CountDownLatch(vmNum);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/CreateVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @SyncThread(level = 300)
    private void createVm(VmInstanceInventory vm, String rootDiskUuid, List<String> nws, List<String> disks) throws ApiSenderException {
        try {
            api.createVmByFullConfig(vm, rootDiskUuid, nws, disks);
        } finally {
            latch.countDown();
        }
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        ImageInventory iminv = api.listImage(null).get(0);
        List<DiskOfferingInventory> dinvs = api.listDiskOffering(null);
        List<L3NetworkInventory> nwinvs = api.listL3Network(null);
        List<String> nws = new ArrayList<String>(nwinvs.size());
        for (L3NetworkInventory nwinv : nwinvs) {
            nws.add(nwinv.getUuid());
        }
        List<String> disks = new ArrayList<String>(1);
        disks.add(dinvs.get(1).getUuid());

        for (int i = 0; i < vmNum; i++) {
            VmInstanceInventory vm = new VmInstanceInventory();
            vm.setDescription("TestVm");
            vm.setName("TestVm");
            vm.setType(VmInstanceConstant.USER_VM_TYPE);
            vm.setInstanceOfferingUuid(ioinv.getUuid());
            vm.setImageUuid(iminv.getUuid());
            createVm(vm, dinvs.get(0).getUuid(), nws, disks);
        }
        latch.await(1, TimeUnit.MINUTES);
    }
}
