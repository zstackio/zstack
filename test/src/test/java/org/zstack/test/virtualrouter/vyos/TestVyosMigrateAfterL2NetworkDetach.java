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
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * @author WeiW
 * @condition use vyos provider
 * <p>
 * 1. detach a l2network from a cluster
 * 2. check the router migrate to another cluster
 * @test confirm port forwarding rules on vm are correct
 */
public class TestVyosMigrateAfterL2NetworkDetach {
    private static final CLogger logger = Utils.getLogger(TestVyosMigrateAfterL2NetworkDetach.class);
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
        deployer = new Deployer("deployerXml/virtualRouter/TestVyosMigrateAfterL2NetworkDetach.xml", con);
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
        String l2Uuid = deployer.l2Networks.get("TestL2Network").getUuid();
        VirtualRouterVmVO vr = dbf.listAll(VirtualRouterVmVO.class).get(0);
        String preClusterUuid = vr.getClusterUuid();

        api.detachL2NetworkFromCluster(l2Uuid, preClusterUuid);
        vr = dbf.listAll(VirtualRouterVmVO.class).get(0);
        String afterClusterUuid = vr.getClusterUuid();

        Assert.assertNotSame(preClusterUuid, afterClusterUuid);
    }
}
