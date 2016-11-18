package org.zstack.test.core.scheduler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.scheduler.SchedulerState;
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by root on 8/23/16.
 */
public class TestSchedulerChangeVmStatus {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        Date date = new Date();
        VmInstanceInventory inv = api.listVmInstances(null).get(0);
        String type = "simple";
        Long startDate = date.getTime() / 1000;
        Integer interval = 5;
        String uuid = inv.getUuid();
        Integer repeatCount = 10;
        // create start vm scheduler, will not take effect at start due to vm status is running
        api.startVmInstanceScheduler(uuid, type, startDate, interval, repeatCount, null);
        // destroy vm
        api.destroyVmInstance(uuid);
        TimeUnit.SECONDS.sleep(5);
        VmInstanceVO vm = dbf.findByUuid(uuid, VmInstanceVO.class);
        Assert.assertNotNull(vm);
        Assert.assertEquals(VmInstanceState.Destroyed, vm.getState());
        SchedulerVO firstRecord = dbf.listAll(SchedulerVO.class).get(0);
        Assert.assertNotNull(firstRecord);
        SchedulerVO scheduler = dbf.findByUuid(firstRecord.getUuid(), SchedulerVO.class);
        Assert.assertNotNull(scheduler);
        Assert.assertEquals(SchedulerState.Disabled.toString(), scheduler.getState());
        // recover vm
        inv = api.recoverVm(inv.getUuid(), null);
        VmInstanceVO vm2 = dbf.findByUuid(uuid, VmInstanceVO.class);
        Assert.assertNotNull(vm2);
        Assert.assertEquals(VmInstanceState.Stopped, vm2.getState());
        SchedulerVO scheduler2 = dbf.findByUuid(firstRecord.getUuid(), SchedulerVO.class);
        Assert.assertNotNull(scheduler2);
        Assert.assertEquals(SchedulerState.Disabled.toString(), scheduler2.getState());
        // expunge vm
        api.destroyVmInstance(inv.getUuid(), null);
        TimeUnit.SECONDS.sleep(2);
        api.expungeVm(inv.getUuid(), null);
        TimeUnit.SECONDS.sleep(3);
        VmInstanceVO vm3 = dbf.findByUuid(uuid, VmInstanceVO.class);
        Assert.assertNull(vm3);
        // check scheduler deleted
        List<SchedulerVO> vos = dbf.listAll(SchedulerVO.class);
        Assert.assertEquals(vos.size(), 0);


    }
}
