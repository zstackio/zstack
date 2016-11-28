package org.zstack.test.eip;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

/**
 * get attachable vm nics for eip and port forwarding are not conflicting
 */
public class TestEipPortForwardingAttachableNic {
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
        deployer = new Deployer("deployerXml/eip/TestEipPortForwardingAttachableNic.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.addSpringConfig("mediator.xml");
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
        PortForwardingRuleInventory pfRule1 = deployer.portForwardingRules.get("pfRule1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        L3NetworkInventory publicl3 = deployer.l3Networks.get("PublicNetwork");
        final L3NetworkInventory l3 = deployer.l3Networks.get("GuestNetwork");
        VmNicInventory targetNic = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l3.getUuid()) ? arg : null;
            }
        });


        EipInventory eip = deployer.eips.get("eip");

        api.attachEip(eip.getUuid(), targetNic.getUuid());
        List<VmNicInventory> nics = api.getPortForwardingAttachableNics(pfRule1.getUuid());
        Assert.assertEquals(0, nics.size());

        api.detachEip(eip.getUuid());
        nics = api.getPortForwardingAttachableNics(pfRule1.getUuid());
        Assert.assertEquals(1, nics.size());
        Assert.assertEquals(targetNic.getUuid(), nics.get(0).getUuid());

        api.attachPortForwardingRule(pfRule1.getUuid(), targetNic.getUuid());
        nics = api.getEipAttachableVmNicsByEipUuid(eip.getUuid());
        Assert.assertEquals(0, nics.size());

        VipInventory vip = api.acquireIp(publicl3.getUuid());
        nics = api.getEipAttachableVmNicsByVipUuid(vip.getUuid());
        Assert.assertEquals(0, nics.size());

        api.detachPortForwardingRule(pfRule1.getUuid());
        nics = api.getEipAttachableVmNicsByEipUuid(eip.getUuid());
        Assert.assertEquals(1, nics.size());
        Assert.assertEquals(targetNic.getUuid(), nics.get(0).getUuid());

        nics = api.getEipAttachableVmNicsByVipUuid(vip.getUuid());
        Assert.assertEquals(1, nics.size());
        Assert.assertEquals(targetNic.getUuid(), nics.get(0).getUuid());
    }
}
