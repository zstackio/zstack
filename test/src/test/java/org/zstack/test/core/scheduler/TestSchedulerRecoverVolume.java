package org.zstack.test.core.scheduler;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.scheduler.SchedulerFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.concurrent.TimeUnit;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 11/17/16.
 */
public class TestSchedulerRecoverVolume {
    ComponentLoader loader;
    Api api;
    @Autowired
    SchedulerFacade scheduler;
    DatabaseFacade dbf;
    CloudBus bus;
    Deployer deployer;
    SessionInventory session;
    VolumeSnapshotKvmSimulator snapshotKvmSimulator;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("SchedulerFacade.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        scheduler = loader.getComponent(SchedulerFacade.class);
        snapshotKvmSimulator = loader.getComponent(VolumeSnapshotKvmSimulator.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException, SchedulerException {
        Assert.assertNotNull(scheduler);
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String volUuid = vm.getRootVolumeUuid();
        Integer interval = 6;
        String type="simple";
        Long startDate=0L;
        Integer count = 4;
        DiskOfferingInventory dinv = new DiskOfferingInventory();
        dinv.setDiskSize(SizeUnit.GIGABYTE.toByte(10));
        dinv.setName("Test");
        dinv.setDescription("Test");
        dinv = api.addDiskOffering(dinv);
        VolumeInventory vinv = api.createDataVolume("TestData", dinv.getUuid());
        Assert.assertEquals(VolumeStatus.NotInstantiated.toString(), vinv.getStatus());
        Assert.assertEquals(VolumeType.Data.toString(), vinv.getType());
        Assert.assertFalse(vinv.isAttached());
        // create scheduler for data volume snapshot
        String DataVolUuid = vinv.getUuid();
        api.attachVolumeToVm(vm.getUuid(), DataVolUuid);
        // first snapshot
        api.createVolumeSnapshotScheduler(DataVolUuid, null, type, startDate, interval, count);
        //destroy volume
        TimeUnit.SECONDS.sleep(1);
        long record1 = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(1,record1);
        api.deleteDataVolume(vinv.getUuid());
        TimeUnit.SECONDS.sleep(19);
        // should not execute job when destroy status
        long record2 = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(1,record2);

        //recover volume, should missing
        api.recoverVolume(vinv.getUuid(), null);
        //TimeUnit.SECONDS.sleep(8);
        // from create 24s, should be triggered 2 times, minus 2 time
        long record3 = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(1,record3);

    }
}
