package org.zstack.test.compute.vm;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * Created by root on 7/30/16.
 */
public class TestStopVmScheduler {
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
        Long startDate = date.getTime()/1000;
        Integer interval = 3;
        Integer repeatCount = 3;
        String uuid = inv.getUuid();
        // stopvm
        api.stopVmInstanceScheduler(uuid, type, startDate, interval, repeatCount);
        TimeUnit.SECONDS.sleep(2);
        VmInstanceVO vm = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm);
        Assert.assertEquals(VmInstanceState.Stopped, vm.getState());
        Assert.assertEquals(null, vm.getHostUuid());
        // startvm
        inv = api.startVmInstance(inv.getUuid());
        Assert.assertEquals(VmInstanceState.Running.toString(), inv.getState());
        TimeUnit.SECONDS.sleep(2);
        // check scheduler stop the vm again
        VmInstanceVO vm2 = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm2);
        Assert.assertEquals(VmInstanceState.Stopped, vm2.getState());
        Assert.assertEquals(null, vm2.getHostUuid());
        HostCapacityVO hvo2 = dbf.findByUuid(inv.getLastHostUuid(), HostCapacityVO.class);
        Assert.assertEquals(0, hvo2.getUsedCpu());
        Assert.assertEquals(0, hvo2.getUsedMemory());

    }
}
