package org.zstack.test.compute.vm;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.CoreGlobalProperty;
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
import org.zstack.identity.IdentityGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestCreate1000Vm2 {
    CLogger logger = Utils.getLogger(TestCreate1000Vm2.class);

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    int vmNum = 500000;
    CountDownLatch latch = new CountDownLatch(vmNum);
    List<Long> timeCost = new ArrayList<Long>();

    @Before
    public void setUp() throws Exception {
        //DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        con.addAllConfigInZstackXml();
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
        api.setTimeout(600);
    }

    @SyncThread(level = 1000)
    private void createVm(VmInstanceInventory vm, String rootDiskUuid, List<String> nws, List<String> disks) throws ApiSenderException {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            api.createVmByFullConfig(vm, rootDiskUuid, nws, disks);
        } finally {
            watch.stop();
            timeCost.add(watch.getTime());
            latch.countDown();
        }
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        CoreGlobalProperty.VM_TRACER_ON = false;
        IdentityGlobalConfig.SESSION_TIMEOUT.updateValue(TimeUnit.HOURS.toSeconds(1000));
        api.prepare();
        InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        ImageInventory iminv = api.listImage(null).get(0);
        List<DiskOfferingInventory> dinvs = api.listDiskOffering(null);
        List<L3NetworkInventory> nwinvs = api.listL3Network(null);
        List<String> nws = new ArrayList<String>(nwinvs.size());
        for (L3NetworkInventory nwinv : nwinvs) {
            nws.add(nwinv.getUuid());
        }

        for (int i = 0; i < vmNum; i++) {
            VmInstanceInventory vm = new VmInstanceInventory();
            vm.setDescription("TestVm");
            vm.setName("TestVm");
            vm.setType(VmInstanceConstant.USER_VM_TYPE);
            vm.setInstanceOfferingUuid(ioinv.getUuid());
            vm.setImageUuid(iminv.getUuid());
            createVm(vm, dinvs.get(0).getUuid(), nws, new ArrayList<String>());
        }
        latch.await(600, TimeUnit.MINUTES);
        long totalTime = 0;
        long minTime = 0;
        long maxTime = 0;
        for (Long t : timeCost) {
            minTime = Math.min(minTime, t);
            maxTime = Math.max(maxTime, t);
            totalTime += t;
        }
        System.out.println(String.format("total time: %s, min time: %s, max time: %s, avg  time: %s",
                TimeUnit.MILLISECONDS.toSeconds(totalTime),
                TimeUnit.MILLISECONDS.toSeconds(minTime),
                TimeUnit.MILLISECONDS.toSeconds(maxTime),
                TimeUnit.MILLISECONDS.toSeconds(totalTime / timeCost.size())
        ));

        /*
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Running);
        long count = q.count();
        Assert.assertEquals(vmNum, count);
        */
        TimeUnit.HOURS.sleep(1000);
    }
}
