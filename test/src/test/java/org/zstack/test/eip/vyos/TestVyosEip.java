package org.zstack.test.eip.vyos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.eip.EipTO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 
 * @author frank
 * 
 * @condition
 * 1. create a vm
 * 2. set eip
 *
 * @test
 * confirm eip works
 */
public class TestVyosEip {
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
        deployer = new Deployer("deployerXml/eip/TestVyosEip.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
        deployer.addSpringConfig("vyos.xml");
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
        VmNicVO nicvo = dbf.findByUuid(eip.getVmNicUuid(), VmNicVO.class);
        Assert.assertEquals(vipvo.getIp(), to.getVipIp());
        Assert.assertEquals(vipvo.getIp(), eip.getVipIp());
        Assert.assertEquals(nicvo.getIp(), eip.getGuestIp());

        // test rules are synced after the vyos vr reboots
        vconfig.dhcpInfoMap.clear();
        vconfig.dnsInfo.clear();
        vconfig.snatInfos.clear();
        VirtualRouterVmVO vr = dbf.listAll(VirtualRouterVmVO.class).get(0);
        api.rebootVmInstance(vr.getUuid());
        Assert.assertFalse(vconfig.dhcpInfoMap.isEmpty());
        Assert.assertFalse(vconfig.dnsInfo.isEmpty());
        Assert.assertFalse(vconfig.snatInfos.isEmpty());
    }
}
