package org.zstack.test.multinodes;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.zone.ZoneInventory;
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
 * delete zone
 */
public class TestMultipleNode1 {
    CLogger logger = Utils.getLogger(TestMultipleNode1.class);
    ComponentLoader loader;
    Deployer deployer;
    NodeManager nodeMgr;
    DatabaseFacade dbf;
    Api api;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/CreateVm.xml");
        deployer.load();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        nodeMgr = new NodeManager();
    }

    private void create100Vm() throws ApiSenderException, InterruptedException {
        final List<Boolean> success = new ArrayList<Boolean>();
        int num = 100;
        final CountDownLatch latch = new CountDownLatch(num);
        InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        ImageInventory iminv = api.listImage(null).get(0);
        List<DiskOfferingInventory> dinvs = api.listDiskOffering(null);
        List<L3NetworkInventory> nwinvs = api.listL3Network(null);
        final List<String> nws = new ArrayList<String>(nwinvs.size());
        for (L3NetworkInventory nwinv : nwinvs) {
            nws.add(nwinv.getUuid());
        }
        final List<String> disks = new ArrayList<String>(1);
        disks.add(dinvs.get(1).getUuid());

        final String rootDiskUuid = dinvs.get(0).getUuid();

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
                        success.add(true);
                    } catch (ApiSenderException e) {
                        logger.warn(e.getMessage(), e);
                    } finally {
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
            create100Vm();
            ZoneInventory zone = deployer.zones.get("TestZone");
            api.deleteZone(zone.getUuid());

            long count = dbf.count(VmInstanceVO.class);
            Assert.assertEquals(0, count);
        } finally {
            nodeMgr.stopNodes(120);
        }
    }
}
