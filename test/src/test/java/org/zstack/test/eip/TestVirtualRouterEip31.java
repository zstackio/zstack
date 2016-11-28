package org.zstack.test.eip;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.eip.EipVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.network.NetworkUtils;

/**
 * 1. create a vm with an eip
 * 2. change the vm's private ip
 * <p>
 * confirm the guest ip of the eip changed to the new one
 */
public class TestVirtualRouterEip31 {
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
        EipInventory eip = deployer.eips.get("eip");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);
        IpRangeInventory ipr = deployer.ipRanges.get("GuestIpRange");

        String newIp = null;
        long s = NetworkUtils.ipv4StringToLong(ipr.getStartIp());
        long e = NetworkUtils.ipv4StringToLong(ipr.getEndIp());

        for (long l = s; s < e; s++) {
            String ip = NetworkUtils.longToIpv4String(l);
            if (!ip.equals(nic.getIp())) {
                newIp = ip;
                break;
            }
        }
        Assert.assertNotNull(newIp);

        api.stopVmInstance(vm.getUuid());
        api.setStaticIp(vm.getUuid(), nic.getL3NetworkUuid(), newIp);
        EipVO eipvo = dbf.findByUuid(eip.getUuid(), EipVO.class);
        Assert.assertEquals(newIp, eipvo.getGuestIp());
    }
}
