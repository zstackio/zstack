package org.zstack.test.virtualrouter.vyos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.ha.HaSystemTags;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.ipsec.IPsecConnectionInventory;
import org.zstack.ipsec.IPsecConnectionVO;
import org.zstack.ipsec.IPsecConstants;
import org.zstack.ipsec.IPsecPeerCidrVO;
import org.zstack.ipsec.vyos.VyosIPsecBackend.CreateIPsecConnectionCmd;
import org.zstack.ipsec.vyos.VyosIPsecBackend.DeleteIPsecConnectionCmd;
import org.zstack.ipsec.vyos.VyosIPsecBackend.IPsecInfo;
import org.zstack.ipsec.vyos.VyosIPsecSimulatorConfig;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.DebugUtils;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * 1. create an ipsec with deleteVipOnFailure system tag set
 * 2. make the ipsec fails to create
 *
 * confirm the vip is deleted
 */
public class TestVyosIPsec4 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    ApplianceVmSimulatorConfig aconfig;
    VyosIPsecSimulatorConfig iconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestVyosIPsec1.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("vyos.xml");
        deployer.addSpringConfig("ipsec.xml");
        deployer.addSpringConfig("ipsecSimulator.xml");
        deployer.addSpringConfig("vyosHa.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        aconfig = loader.getComponent(ApplianceVmSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        iconfig = loader.getComponent(VyosIPsecSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory publicNw = deployer.l3Networks.get("PublicNetwork");
        L3NetworkInventory guestL3 = deployer.l3Networks.get("GuestNetwork");
        VipInventory vip = api.acquireIp(publicNw.getUuid());

        IPsecConnectionInventory inv = new IPsecConnectionInventory();
        inv.setName("test");
        inv.setDescription("test");
        inv.setTransformProtocol("ah");
        inv.setL3NetworkUuid(guestL3.getUuid());
        inv.setPeerAddress("172.20.0.1");
        inv.setAuthMode("psk");
        inv.setAuthKey("test");
        inv.setVipUuid(vip.getUuid());
        inv.setIkeAuthAlgorithm("md5");
        inv.setIkeEncryptionAlgorithm("aes-256");
        inv.setIkeDhGroup(3);
        inv.setPolicyAuthAlgorithm("sha1");
        inv.setPolicyEncryptionAlgorithm("aes-128");
        inv.setPfs("dh-group19");
        inv.setPolicyMode("tunnel");

        List<String> peerCidrs = asList("10.2.1.0/24", "10.3.1.0/24");
        iconfig.createIPsecConnectionSuccess = false;
        boolean s = false;
        try {
            api.createIPsecConnection(inv, peerCidrs, null);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
        Assert.assertFalse(dbf.isExist(vip.getUuid(), VipVO.class));
    }
}
