package org.zstack.test.multinodes;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.AsyncThread;
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
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * start 5 nodes, create 100 vm
 * async stop a node
 * create another 100 vm
 * <p>
 * TODO: not working now
 */
public class TestMultipleNode3 {
    CLogger logger = Utils.getLogger(TestMultipleNode3.class);
    ComponentLoader loader;
    Deployer deployer;
    NodeManager nodeMgr;
    Api api;
    InstanceOfferingInventory ioinv;
    ImageInventory iminv;
    List<DiskOfferingInventory> dinvs;
    List<L3NetworkInventory> nwinvs;
    List<String> nws;
    List<String> disks;
    String rootDiskUuid;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/CreateVm.xml");
        deployer.load();
        loader = deployer.getComponentLoader();
        nodeMgr = new NodeManager();
    }

    private void create100Vm() throws ApiSenderException, InterruptedException {
        final List<Boolean> success = new ArrayList<Boolean>();
        int num = 100;
        final CountDownLatch latch = new CountDownLatch(num);

        for (int i = 0; i < num; i++) {
            final VmInstanceInventory vm = new VmInstanceInventory();
            vm.setDescription("TestVm");
            vm.setName("TestVm");
            vm.setType(VmInstanceConstant.USER_VM_TYPE);
            vm.setInstanceOfferingUuid(ioinv.getUuid());
            vm.setImageUuid(iminv.getUuid());
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    try {
                        api.createVmByFullConfig(vm, rootDiskUuid, nws, disks);
                    } catch (ApiSenderException e) {
                        logger.warn(e.getMessage(), e);
                    } finally {
                        success.add(true);
                        latch.countDown();
                    }
                }
            }.run();
        }

        latch.await(3, TimeUnit.MINUTES);

        Assert.assertEquals(num, success.size());
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        try {
            nodeMgr.setProperties("-DbFacadeDataSource.maxPoolSize=50").createNodes(3, 300);
            deployer.prepareApiClient().deploy();
            api = deployer.getApi();
            api.setTimeout(300);

            ioinv = api.listInstanceOffering(null).get(0);
            iminv = api.listImage(null).get(0);
            dinvs = api.listDiskOffering(null);
            nwinvs = api.listL3Network(null);
            nws = new ArrayList<String>(nwinvs.size());
            for (L3NetworkInventory nwinv : nwinvs) {
                nws.add(nwinv.getUuid());
            }
            disks = new ArrayList<String>(1);
            disks.add(dinvs.get(1).getUuid());
            rootDiskUuid = dinvs.get(0).getUuid();

            create100Vm();
            nodeMgr.asyncStopNode(1, 120);
            create100Vm();
        } finally {
            nodeMgr.stopNodes(120);
        }
    }
}
