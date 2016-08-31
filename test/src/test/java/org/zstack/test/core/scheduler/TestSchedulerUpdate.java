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
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by root on 7/19/16.
 */
public class TestSchedulerUpdate {
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
        Date date = new Date();
        Assert.assertNotNull(scheduler);
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String volUuid = vm.getRootVolumeUuid();
        Integer interval = 3;
        Integer repeatCount = 10;
        String type = "simple";
        Long startDate = date.getTime()/1000;
        api.createVolumeSnapshotScheduler(volUuid, session, type, startDate, interval, repeatCount);
        TimeUnit.SECONDS.sleep(1);
        long record = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(1,record);
        SchedulerVO firstRecord = dbf.listAll(SchedulerVO.class).get(0);
        api.updateScheduler(firstRecord.getUuid(),"test update", "new description", session);
        SchedulerVO secondRecord= dbf.listAll(SchedulerVO.class).get(0);
        Assert.assertEquals(secondRecord.getSchedulerDescription(), "new description");
        Assert.assertEquals(secondRecord.getSchedulerName(), "test update");

        api.changeSchedulerState(firstRecord.getUuid(), "disable", session);
        SchedulerVO pauseRecord = dbf.listAll(SchedulerVO.class).get(0);
        Assert.assertEquals(pauseRecord.getState(), "Disabled");
        TimeUnit.SECONDS.sleep(3);
        long pauseCount = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(1,pauseCount);

        api.changeSchedulerState(firstRecord.getUuid(), "enable", session);
        SchedulerVO resumeRecord = dbf.listAll(SchedulerVO.class).get(0);
        Assert.assertEquals(resumeRecord.getState(), "Enabled");
        TimeUnit.SECONDS.sleep(6);
        long resumeCount = dbf.count(VolumeSnapshotVO.class);
        //resume will trigger immediately, so
        Assert.assertEquals(4,resumeCount);

        api.changeResourceOwner(vm.getUuid(), AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        TimeUnit.SECONDS.sleep(4);
        long changeCount = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(4,changeCount);
        SchedulerVO changeRecord = dbf.listAll(SchedulerVO.class).get(0);
        Assert.assertEquals(changeRecord.getState(), "Disabled");


    }
}
