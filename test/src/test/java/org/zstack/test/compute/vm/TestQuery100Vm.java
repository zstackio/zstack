package org.zstack.test.compute.vm;

import junit.framework.Assert;
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
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestQuery100Vm {
    CLogger logger = Utils.getLogger(TestQuery100Vm.class);

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    int vmNum = 100;
    int queryTimes = 20000;
    CountDownLatch latch = new CountDownLatch(vmNum);
    CountDownLatch latch2 = new CountDownLatch(queryTimes);
    List<Long> timeCost = new ArrayList<Long>();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/CreateVm1000.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @SyncThread(level = 100)
    private void createVm(VmInstanceInventory vm, String rootDiskUuid, List<String> nws, List<String> disks) throws ApiSenderException {
        try {
            api.createVmByFullConfig(vm, rootDiskUuid, nws, disks);
        } finally {
            latch.countDown();
        }
    }

    @SyncThread(level = 1000)
    private void queryVm() throws ApiSenderException {
        try {
            long s = System.currentTimeMillis();
            APIQueryVmInstanceMsg msg = new APIQueryVmInstanceMsg();
            msg.addQueryCondition("name", QueryOp.EQ, "vm");
            APIQueryVmInstanceReply reply = api.query(msg, APIQueryVmInstanceReply.class);
            Assert.assertEquals(vmNum, reply.getInventories().size());
            timeCost.add(System.currentTimeMillis() - s);
        } finally {
            latch2.countDown();
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

        for (int i = 0; i < vmNum; i++) {
            VmInstanceInventory vm = new VmInstanceInventory();
            vm.setDescription("TestVm");
            vm.setName("vm");
            vm.setType(VmInstanceConstant.USER_VM_TYPE);
            vm.setInstanceOfferingUuid(ioinv.getUuid());
            vm.setImageUuid(iminv.getUuid());
            createVm(vm, dinvs.get(0).getUuid(), nws, new ArrayList<String>());
        }
        latch.await(10, TimeUnit.MINUTES);

        long qs = System.currentTimeMillis();
        for (int i = 0; i < queryTimes; i++) {
            queryVm();
        }
        latch2.await(20, TimeUnit.MINUTES);
        long qe = System.currentTimeMillis();

        long totalTime = 0;
        long minTime = 0;
        long maxTime = 0;
        for (Long t : timeCost) {
            minTime = Math.min(minTime, t);
            maxTime = Math.max(maxTime, t);
            totalTime += t;
        }

        System.out.println(String.format("total time: %s, min time: %s, max time: %s, avg  time: %s",
                TimeUnit.MILLISECONDS.toSeconds(qe - qs),
                TimeUnit.MILLISECONDS.toSeconds(minTime),
                TimeUnit.MILLISECONDS.toSeconds(maxTime),
                TimeUnit.MILLISECONDS.toSeconds(totalTime / timeCost.size())
        ));
    }
}
