package org.zstack.test.eip;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * get attachable vm nic for eip
 */
public class TestVirtualRouterEip23 {
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
        L3NetworkInventory publicl3 = deployer.l3Networks.get("PublicNetwork");
        VipInventory vip = api.acquireIp(publicl3.getUuid());
        L3NetworkInventory l3 = deployer.l3Networks.get("GuestNetwork");
        List<VmNicInventory> nics = api.getEipAttachableVmNicsByVipUuid(vip.getUuid());
        List<VmNicInventory> guestNics = api.getL3NetworkVmNics(l3.getUuid());
        Assert.assertEquals(guestNics.size(), nics.size());

        EipInventory eip = deployer.eips.get("eip");

        api.detachEip(eip.getUuid());

        nics = api.getEipAttachableVmNicsByEipUuid(eip.getUuid());
        Assert.assertEquals(1, nics.size());
        VmNicInventory nic = nics.get(0);
        Assert.assertEquals(l3.getUuid(), nic.getL3NetworkUuid());

        nics = api.getEipAttachableVmNicsByVipUuid(vip.getUuid());
        Assert.assertEquals(1, nics.size());
        nic = nics.get(0);
        Assert.assertEquals(l3.getUuid(), nic.getL3NetworkUuid());

        api.destroyVmInstance(nic.getVmInstanceUuid());
        nics = api.getEipAttachableVmNicsByEipUuid(eip.getUuid());
        Assert.assertEquals(0, nics.size());

    }
}
