package org.zstack.test.eip.flatnetwork;

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
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * 1. create two vm on two l3 networks
 * 2. one network has flat network provider and another network uses virtual router
 * 3. create an eip
 *
 * confirm the eip is attachable to two vms
 *
 * 4. attach/detach the eip so the eip is created on the backend
 *
 * confirm the eip is only attachable to the vm that it used to attach to
 */
public class TestFlatNetworkEip17 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;
    FlatNetworkServiceSimulatorConfig fconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/eip/TestFlatNetworkEip17.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("flatNetworkServiceSimulator.xml");
        deployer.addSpringConfig("flatNetworkProvider.xml");
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
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        EipInventory eip = deployer.eips.get("eip");

        List<VmNicInventory> nics = api.getEipAttachableVmNicsByEipUuid(eip.getUuid());
        Assert.assertEquals(2, nics.size());
        nics = api.getEipAttachableVmNicsByVipUuid(eip.getVipUuid());
        Assert.assertEquals(2, nics.size());

        // this creates the EIP on the backend
        api.attachEip(eip.getUuid(), vm.getVmNics().get(0).getUuid());
        api.detachEip(eip.getUuid());

        nics = api.getEipAttachableVmNicsByEipUuid(eip.getUuid());
        Assert.assertEquals(1, nics.size());
        VmNicInventory nic = nics.get(0);
        Assert.assertEquals(vm.getVmNics().get(0).getUuid(), nic.getUuid());

        nics = api.getEipAttachableVmNicsByVipUuid(eip.getVipUuid());
        Assert.assertEquals(1, nics.size());
        nic = nics.get(0);
        Assert.assertEquals(vm.getVmNics().get(0).getUuid(), nic.getUuid());
    }
}
