package org.zstack.test.eip.vyos;

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
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.eip.EipTO;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * 1. two eips for two VMs on different L3s
 * 2. detach an eip
 *
 * confirm the detached EIP is only candidate to the VM it's detached from
 *
 * see BUG https://github.com/zxwing/premium/issues/1879
 */
public class TestVyosEip1 {
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
        deployer = new Deployer("deployerXml/eip/TestVyosEip1.xml", con);
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
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        EipInventory eip1 = deployer.eips.get("eip1");

        api.detachEip(eip1.getUuid());
        List<VmNicInventory> nics = api.getEipAttachableVmNicsByEipUuid(eip1.getUuid());
        Assert.assertEquals(1, nics.size());
        VmNicInventory nic = nics.get(0);
        Assert.assertEquals(nic.getUuid(), vm1.getVmNics().get(0).getUuid());
    }
}
