package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

public class TestVirtualRouterDMZ {
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
        deployer = new Deployer("deployerXml/virtualRouter/virtualRouterDMZ.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
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
    public void test() {
        List<ApplianceVmVO> vrs = dbf.listAll(ApplianceVmVO.class);
        Assert.assertEquals(2, vrs.size());

        ApplianceVmVO pubVR = null;
        ApplianceVmVO snatVR = null;
        for (ApplianceVmVO vr : vrs) {
            if (vr.getVmNics().size() == 3) {
                snatVR = vr;
            }
            if (vr.getVmNics().size() == 2) {
                pubVR = vr;
            }
        }

        Assert.assertNotNull(pubVR);
        Assert.assertNotNull(snatVR);

        L3NetworkInventory pubL3 = deployer.l3Networks.get("publicNetwork");
        L3NetworkInventory snatL3 = deployer.l3Networks.get("TestL3Network2");

        for (VmNicVO nic : pubVR.getVmNics()) {
            if (VirtualRouterNicMetaData.isGuestNic(nic)) {
                Assert.assertEquals(nic.getL3NetworkUuid(), pubL3.getUuid());
            }
        }

        for (VmNicVO nic : snatVR.getVmNics()) {
            if (VirtualRouterNicMetaData.isGuestNic(nic)) {
                Assert.assertEquals(nic.getL3NetworkUuid(), snatL3.getUuid());
            }
        }
    }
}
