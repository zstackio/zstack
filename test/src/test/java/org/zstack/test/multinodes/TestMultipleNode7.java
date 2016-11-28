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
 * start 1 node, start creating vms, start another node
 * delete zone
 */
public class TestMultipleNode7 {
    CLogger logger = Utils.getLogger(TestMultipleNode7.class);
    ComponentLoader loader;
    Deployer deployer;
    NodeManager nodeMgr;
    DatabaseFacade dbf;
    CountDownLatch latch = new CountDownLatch(1);
    Api api;
    boolean allNodeStarted = false;
    List<ApiSenderException> failures = new ArrayList<ApiSenderException>();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/CreateVm.xml");
        deployer.load();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        nodeMgr = new NodeManager();
    }

    @AsyncThread
    private void createVm() throws ApiSenderException, InterruptedException {
        try {
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

            while (!allNodeStarted) {
                final VmInstanceInventory vm = new VmInstanceInventory();
                vm.setDescription("TestVm");
                vm.setName("TestVm");
                vm.setType(VmInstanceConstant.USER_VM_TYPE);
                vm.setInstanceOfferingUuid(ioinv.getUuid());
                vm.setImageUuid(iminv.getUuid());
                try {
                    api.createVmByFullConfig(vm, rootDiskUuid, nws, disks);
                } catch (ApiSenderException e) {
                    logger.warn(e.getMessage(), e);
                    failures.add(e);
                }
            }
        } finally {
            latch.countDown();
        }
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        try {
            nodeMgr.setProperties("-DbFacadeDataSource.maxPoolSize=50").createNodes(1, 300);
            deployer.prepareApiClient().deploy();
            api = deployer.getApi();
            api.setTimeout(300);
            createVm();
            nodeMgr.setProperties("-DbFacadeDataSource.maxPoolSize=50").createNodes(1, 300);
            allNodeStarted = true;
            latch.await(30, TimeUnit.SECONDS);
            Assert.assertEquals(0, failures.size());
            long vmCount = dbf.count(VmInstanceVO.class);
            logger.debug(String.format("totally created %s vms", vmCount));
            ZoneInventory zone = deployer.zones.get("TestZone");
            api.deleteZone(zone.getUuid());

            long count = dbf.count(VmInstanceVO.class);
            Assert.assertEquals(0, count);
        } finally {
            nodeMgr.stopNodes(120);
        }
    }
}
