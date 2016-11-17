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
import org.zstack.header.core.scheduler.SchedulerVO;
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
 * Created by root on 7/11/16.
 */
public class TestSchedulerCreateVolumeSnapshot {
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
        Integer interval = 3;
        String type="simple";
        Long startDate=0L;
        String schedulerUuid = api.createVolumeSnapshotScheduler(volUuid, session, type, startDate, interval, null);
        TimeUnit.SECONDS.sleep(2);
        long record = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(1,record);

        //test change volume status
        api.deleteScheduler(schedulerUuid, null);
        long record0 = dbf.count(SchedulerVO.class);
        Assert.assertEquals(0,record0);
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
        api.createVolumeSnapshotScheduler(DataVolUuid, null, type, startDate, interval, null);
        // test quick create volume scheduler will be ignore
        api.createVolumeSnapshotScheduler(DataVolUuid, null, type, startDate, interval, null);
        //destroy volume
        TimeUnit.SECONDS.sleep(2);
        long record1 = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(2,record1);
        api.deleteDataVolume(vinv.getUuid());
        TimeUnit.SECONDS.sleep(4);
        long record2 = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(2,record2);

        //recover volume
        api.recoverVolume(vinv.getUuid(), null);
        TimeUnit.SECONDS.sleep(2);
        long record3 = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(4,record3);

        //expunge volume
        api.deleteDataVolume(vinv.getUuid());
        api.expungeDataVolume(vinv.getUuid(), null);
        TimeUnit.SECONDS.sleep(3);
        long record4 = dbf.count(VolumeSnapshotVO.class);
        //only leave the first root volume record
        Assert.assertEquals(1,record4);

        // check schedulerVO
        long record5 = dbf.count(SchedulerVO.class);
        Assert.assertEquals(0,record5);


    }

}
