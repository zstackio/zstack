package org.zstack.test.virtualrouter.vyos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.network.service.portforwarding.PortForwardingProtocolType;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.virtualrouter.PortForwardingRuleTestValidator;
import org.zstack.test.virtualrouter.VipTestValidator;

/**
 * @author frank
 * @condition use vyos provider
 * <p>
 * 1. create a vm
 * 2. acquire a public ip: pub1
 * 3. set port forwarding to vm using pub1
 * @test confirm port forwarding rules on vm are correct
 */
public class TestVyosAfterL2NetworkDetach {
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
        deployer = new Deployer("deployerXml/virtualRouter/TestVyosAfterL2NetworkDetach.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("vyos.xml");
        deployer.addSpringConfig("PortForwarding.xml");
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
        L3NetworkInventory publicNw = deployer.l3Networks.get("PublicNetwork");
        String l2Uuid = deployer.l2Networks.get("TestL2Network").getUuid();
        String clusterUuid = deployer.clusters.get("Cluster1").getUuid();
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VirtualRouterVmVO vr = dbf.listAll(VirtualRouterVmVO.class).get(0);
        Assert.assertNotNull(vr);

        api.detachL2NetworkFromCluster(l2Uuid, clusterUuid);

        vr = dbf.listAll(VirtualRouterVmVO.class).get(0);
        Assert.assertNull(vr);
        Assert.assertNull(vm.getVmNics());
    }
}
