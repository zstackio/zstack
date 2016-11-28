package org.zstack.test.eip;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands;
import org.zstack.network.service.virtualrouter.eip.EipTO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * @author frank
 * @condition 1. create a vm1
 * 2. set eip
 * 3. create another vm2
 * 4. detach eip from vm1
 * 5. attach eip to vm2
 * @test confirm eip attached to vm2
 */
public class TestVirtualRouterEip15 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/eip/TestVirtualRouterEip.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        Assert.assertEquals(1, vconfig.eips.size());
        EipTO to = vconfig.eips.get(0);
        EipInventory eip = deployer.eips.get("eip");
        VipVO vipvo = dbf.findByUuid(eip.getVipUuid(), VipVO.class);
        Assert.assertEquals(vipvo.getIp(), to.getVipIp());
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm2 = api.createVmFromClone(vm);

        api.detachEip(eip.getUuid());
        Assert.assertEquals(1, vconfig.removedEips.size());
        to = vconfig.removedEips.get(0);
        Assert.assertEquals(vipvo.getIp(), to.getVipIp());
        EipVO evo = dbf.findByUuid(eip.getUuid(), EipVO.class);
        Assert.assertNull(evo.getVmNicUuid());
        Assert.assertEquals(1, vconfig.removedVips.size());
        VirtualRouterCommands.VipTO vto = vconfig.removedVips.get(0);
        Assert.assertEquals(vipvo.getIp(), vto.getIp());

        vconfig.eips.clear();
        VmNicInventory nic2 = vm2.getVmNics().get(0);
        eip = api.attachEip(eip.getUuid(), nic2.getUuid());
        Assert.assertEquals(nic2.getUuid(), eip.getVmNicUuid());
        Assert.assertEquals(1, vconfig.eips.size());
        to = vconfig.eips.get(0);
        Assert.assertEquals(vipvo.getIp(), to.getVipIp());
    }
}
