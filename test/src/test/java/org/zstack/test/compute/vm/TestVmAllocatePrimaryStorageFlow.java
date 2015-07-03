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
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @deprecated
 */
public class TestVmAllocatePrimaryStorageFlow {
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
        deployer = new Deployer("deployerXml/vm/TestVmAllocatePrimaryStorageFlow.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException,ApiSenderException {
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
        final List<DiskOfferingInventory> disks = api.listDiskOffering(null);
        for (DiskOfferingInventory dinv : disks) {
            if (dinv.getAllocatorStrategy() == null) {
                dinv.setAllocatorStrategy(PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE);
            }
        }
        spec.setDataDiskOfferings(disks);
        spec.getImageSpec().setInventory(iminv);
        spec.setDestHost(hinv);

        PrimaryStorageInventory pinv = deployer.primaryStorages.get("TestPrimaryStorage");
        final long availSize = pinv.getAvailableCapacity();
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler() {
            @Override
            public void handle(Map data) {
                try {
                    VmInstanceSpec ret = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                    List<VolumeSpec> vspecs = ret.getVolumeSpecs();
                    PrimaryStorageVO vo = dbf.findByUuid(vspecs.get(0).getPrimaryStorageInventory().getUuid(), PrimaryStorageVO.class);
                    long totalUsed = 0;
                    totalUsed += iminv.getSize();
                    for (DiskOfferingInventory dinv : disks) {
                        totalUsed += dinv.getDiskSize();
                    }
                    Assert.assertEquals(totalUsed, availSize - vo.getCapacity().getAvailableCapacity());
                    isSuccess = true;
                } finally {
                    latch.countDown();
                }
            }
        }).error(new FlowErrorHandler() {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                isSuccess = false;
            }
        }).start();

        latch.await(2, TimeUnit.MINUTES);
        Assert.assertTrue(isSuccess);
    }
}
