package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmAllocatePrimaryStorageFlow;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestVmAllocatePrimaryStorageFlowFailure {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    CountDownLatch latch = new CountDownLatch(1);
    boolean isSuccess = false;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestVmAllocatePrimaryStorageFlowFailure.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain().then(new VmAllocatePrimaryStorageFlow());

        SimpleQuery<HostVO> query = dbf.createQuery(HostVO.class);
        query.add(HostVO_.name, Op.EQ, "TestHost2");
        final HostVO hvo = query.find();
        HostInventory hinv = HostInventory.valueOf(hvo);

        final InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        final ImageInventory iminv = api.listImage(null).get(0);
        VmInstanceInventory vminv = new VmInstanceInventory();
        vminv.setInstanceOfferingUuid(ioinv.getUuid());
        vminv.setImageUuid(iminv.getUuid());
        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setVmInventory(vminv);

        SimpleQuery<DiskOfferingVO> dquery = dbf.createQuery(DiskOfferingVO.class);
        dquery.add(DiskOfferingVO_.name, Op.EQ, "TestDiskOffering1");
        DiskOfferingVO dvo1 = dquery.find();
        Assert.assertNotNull(dvo1);
        final DiskOfferingInventory dinv1 = DiskOfferingInventory.valueOf(dvo1);

        dquery = dbf.createQuery(DiskOfferingVO.class);
        dquery.add(DiskOfferingVO_.name, Op.EQ, "TestDiskOffering2");
        DiskOfferingVO dvo2 = dquery.find();
        Assert.assertNotNull(dvo2);
        final DiskOfferingInventory dinv2 = DiskOfferingInventory.valueOf(dvo2);

        dquery = dbf.createQuery(DiskOfferingVO.class);
        dquery.add(DiskOfferingVO_.name, Op.EQ, "TestDiskOffering3");
        DiskOfferingVO dvo3 = dquery.find();
        Assert.assertNotNull(dvo3);
        final DiskOfferingInventory dinv3 = DiskOfferingInventory.valueOf(dvo3);

        SimpleQuery<PrimaryStorageVO> pquery = dbf.createQuery(PrimaryStorageVO.class);
        pquery.add(PrimaryStorageVO_.name, Op.EQ, "TestPrimaryStorage1");
        final PrimaryStorageVO pvo1 = pquery.find();
        Assert.assertNotNull(pvo1);

        pquery = dbf.createQuery(PrimaryStorageVO.class);
        pquery.add(PrimaryStorageVO_.name, Op.EQ, "TestPrimaryStorage2");
        final PrimaryStorageVO pvo2 = pquery.find();
        Assert.assertNotNull(pvo2);

        pquery = dbf.createQuery(PrimaryStorageVO.class);
        pquery.add(PrimaryStorageVO_.name, Op.EQ, "TestPrimaryStorage3");
        final PrimaryStorageVO pvo3 = pquery.find();
        Assert.assertNotNull(pvo3);

        pquery = dbf.createQuery(PrimaryStorageVO.class);
        pquery.add(PrimaryStorageVO_.name, Op.EQ, "TestPrimaryStorageForRootVolume");
        final PrimaryStorageVO pvo4 = pquery.find();
        Assert.assertNotNull(pvo4);

        final List<DiskOfferingInventory> disks = new ArrayList<DiskOfferingInventory>(3);
        disks.add(dinv1);
        disks.add(dinv2);
        disks.add(dinv3);
        spec.setDataDiskOfferings(disks);

        ImageInventory img = deployer.images.get("TestImage");
        spec.getImageSpec().setInventory(img);

        spec.setDestHost(hinv);

        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
                isSuccess = false;
                latch.countDown();
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                try {
                    SimpleQuery<PrimaryStorageCapacityVO> query = dbf.createQuery(PrimaryStorageCapacityVO.class);
                    query.add(PrimaryStorageCapacityVO_.availableCapacity, Op.NOT_EQ, PrimaryStorageCapacityVO_.totalCapacity);
                    long count = query.count();
                    Assert.assertEquals(0, count);
                    isSuccess = true;
                } finally {
                    latch.countDown();
                }
            }
        }).start();

        latch.await(2, TimeUnit.MINUTES);
        Assert.assertTrue(isSuccess);
    }
}
