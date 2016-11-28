package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO;
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefVO;
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefVO_;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * @author frank
 * @condition 1. create a vm with port forwarding rule using deployer
 * 2. destroy vr
 * 3. create a new vr
 * @test confirm port forwarding rule is on the new vr
 */
public class TestVirtualRouterPortForwarding33 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    ApplianceVmSimulatorConfig aconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterPortForwarding2.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        aconfig = loader.getComponent(ApplianceVmSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0);
        api.destroyVmInstance(vr.getUuid());

        vconfig.portForwardingRules.clear();
        vconfig.vips.clear();
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.createVmFromClone(vm);
        vr = dbf.listAll(ApplianceVmVO.class).get(0);

        SimpleQuery<VirtualRouterPortForwardingRuleRefVO> q = dbf.createQuery(VirtualRouterPortForwardingRuleRefVO.class);
        q.add(VirtualRouterPortForwardingRuleRefVO_.virtualRouterVmUuid, Op.EQ, vr.getUuid());
        List<VirtualRouterPortForwardingRuleRefVO> refs = q.list();
        Assert.assertEquals(1, refs.size());

        PortForwardingRuleInventory pfRule1 = deployer.portForwardingRules.get("pfRule1");
        VipVO vipvo = dbf.findByUuid(pfRule1.getVipUuid(), VipVO.class);
        VipInventory vip = VipInventory.valueOf(vipvo);
        Assert.assertEquals(1, vconfig.portForwardingRules.size());
        PortForwardingRuleTO rule = vconfig.portForwardingRules.get(0);
        PortForwardingRuleTestValidator validator = new PortForwardingRuleTestValidator();
        Assert.assertTrue(validator.compare(rule, pfRule1));
        VipTestValidator.validateWithoutCheckOwnerEthernetMac(vconfig.vips, vip);

        validator.noFirewall(aconfig, pfRule1);
    }
}
