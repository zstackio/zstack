package org.zstack.test.compute.vm;

import junit.framework.Assert;
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
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * Created by root on 11/3/16.
 */
public class TestPauseVm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception{
        WebBeanConstructor con = new WebBeanConstructor();
        DBUtil.reDeployDB();

        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvmIso.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        //deployer = new Deployer("deployerXml/vm/TestChangeVmInstanceOffering.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory inv = deployer.vms.get("TestVm");
        inv = api.pauseVmInstance(inv.getUuid());
        Assert.assertEquals(VmInstanceState.Paused.toString(), inv.getState());
        VmInstanceVO vm = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm);
        Assert.assertEquals(VmInstanceState.Paused, vm.getState());
        Assert.assertNotNull(vm.getHostUuid());
        //stop a suspended vm
        inv = api.stopVmInstance(inv.getUuid());
        Assert.assertEquals(VmInstanceState.Stopped.toString(), inv.getState());
        vm = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm);
        Assert.assertEquals(VmInstanceState.Stopped, vm.getState());
        Assert.assertEquals(null,vm.getHostUuid());
    }
}
