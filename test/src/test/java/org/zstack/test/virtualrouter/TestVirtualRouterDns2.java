package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.DnsInfo;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.RemoveDnsCmd;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

/**
 * 1. add a dns
 * <p>
 * confirm the dns added in vr
 * <p>
 * 2. remove the dns
 * <p>
 * confirm the dns removed in vr
 */
public class TestVirtualRouterDns2 {
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
        deployer = new Deployer("deployerXml/virtualRouter/virtualRouterDns.xml", con);
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
    public void test() throws ApiSenderException {
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network2");
        vconfig.dnsInfo.clear();
        final String newDns = "1.1.1.1";
        api.addDns(l3.getUuid(), newDns);
        String dns = CollectionUtils.find(vconfig.dnsInfo, new Function<String, DnsInfo>() {
            @Override
            public String call(DnsInfo arg) {
                return arg.getDnsAddress().equals(newDns) ? newDns : null;
            }
        });
        Assert.assertNotNull(dns);

        api.removeDnsFromL3Network(newDns, l3.getUuid());
        Assert.assertFalse(vconfig.removeDnsCmds.isEmpty());
        RemoveDnsCmd cmd = vconfig.removeDnsCmds.get(0);
        Assert.assertEquals(newDns, cmd.getDns().get(0).getDnsAddress());
    }
}
