package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SNATInfo;
import org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

public class TestVirtualRouterSNAT {
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
        deployer = new Deployer("deployerXml/virtualRouter/virtualRouterSNAT.xml", con);
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
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network2");
        // reload l3network making sure all context are in it
        L3NetworkVO l3vo = dbf.findByUuid(l3.getUuid(), L3NetworkVO.class);
        l3 = L3NetworkInventory.valueOf(l3vo);
        IpRangeInventory ir = l3.getIpRanges().get(0);
        SNATInfo info = vconfig.snatInfos.get(0);
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(dbf.listAll(VirtualRouterVmVO.class).get(0));
        VmNicInventory nic = null;
        for (VmNicInventory n : vr.getVmNics()) {
            if (!VirtualRouterNicMetaData.isManagementNic(n) && !VirtualRouterNicMetaData.isPublicNic(n)) {
                nic = n;
                break;
            }
        }

        Assert.assertEquals(info.getPrivateNicIp(), ir.getGateway());
        Assert.assertEquals(info.getPrivateNicMac(), nic.getMac());
        Assert.assertEquals(info.getPublicNicMac(), vr.getPublicNic().getMac());
        Assert.assertEquals(info.getSnatNetmask(), ir.getNetmask());
    }
}
