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
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.vip.VipVO;
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
 *         <p>
 *         1. create a vm with an eip
 *         2. detach the eip nic
 *         <p>
 *         confirm eip deleted
 *         confirm the vmNicUuid and guestIp set to null on the eip
 */
public class TestVirtualRouterEip29 {
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

        EipInventory eip = deployer.eips.get("eip");
        VipVO vipvo = dbf.findByUuid(eip.getVipUuid(), VipVO.class);
        VmNicVO nicvo = dbf.findByUuid(eip.getVmNicUuid(), VmNicVO.class);

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);
        api.detachNic(nic.getUuid());

        Assert.assertEquals(1, vconfig.removedEips.size());
        EipTO to = vconfig.removedEips.get(0);
        Assert.assertEquals(vipvo.getIp(), to.getVipIp());
        Assert.assertEquals(nicvo.getIp(), to.getGuestIp());

        EipVO eipvo = dbf.findByUuid(eip.getUuid(), EipVO.class);
        Assert.assertNull(eipvo.getVmNicUuid());
        Assert.assertNull(eipvo.getGuestIp());

        api.rebootVmInstance(vm.getUuid());
    }
}
