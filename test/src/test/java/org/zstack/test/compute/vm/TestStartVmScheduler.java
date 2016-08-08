package org.zstack.test.compute.vm;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
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
public class TestStartVmScheduler {
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
        VmInstanceInventory inv = api.listVmInstances(null).get(0);
        api.stopVmInstance(inv.getUuid());
        VmInstanceVO vm = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm);
        Assert.assertEquals(VmInstanceState.Stopped, vm.getState());
        Assert.assertEquals(null, vm.getHostUuid());
        //inv = api.startVmInstance(inv.getUuid());

        Date date = new Date();
        String type = "simple";
        Long startDate = date.getTime() + 1000;
        Integer interval = 3;
        Integer repeatCount = 3;
        String vmUuid = inv.getUuid();
        api.startVmInstanceScheduler(vmUuid, type, startDate, interval, repeatCount);
        TimeUnit.SECONDS.sleep(2);

        VmInstanceVO vm2 = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm2);
        Assert.assertEquals(VmInstanceState.Running, vm2.getState());
        Assert.assertNotNull(vm2.getHostUuid());

        api.stopVmInstance(inv.getUuid());
        VmInstanceVO vm3 = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm3);
        Assert.assertEquals(VmInstanceState.Stopped, vm3.getState());
        Assert.assertEquals(null, vm3.getHostUuid());

        TimeUnit.SECONDS.sleep(3);

        VmInstanceVO vm4 = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm4);
        Assert.assertEquals(VmInstanceState.Running, vm4.getState());
        Assert.assertNotNull(vm4.getHostUuid());
    }
}
