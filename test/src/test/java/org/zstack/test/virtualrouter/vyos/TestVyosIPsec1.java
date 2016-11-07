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
import org.zstack.ipsec.IPsecConnectionInventory;
import org.zstack.ipsec.IPsecConnectionVO;
import org.zstack.ipsec.vyos.VyosIPsecBackend.CreateIPsecConnectionCmd;
import org.zstack.ipsec.vyos.VyosIPsecBackend.IPsecInfo;
import org.zstack.ipsec.vyos.VyosIPsecSimulatorConfig;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
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
 *  test create ipsec connection
 */
public class TestVyosIPsec1 {
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

    private void compare(IPsecConnectionInventory inv1, IPsecConnectionInventory inv2, boolean comparePeerCidrs) {
        Assert.assertEquals(String.format("different name[%s, %s]", inv1.getName(),
                inv2.getName()), inv1.getName(), inv2.getName());
        Assert.assertEquals(String.format("different description[%s, %s]",
                inv1.getDescription(), inv2.getDescription()), inv1.getDescription(), inv2.getDescription());
        Assert.assertEquals(String.format("different l3NetworkUuid[%s, %s]",
                inv1.getL3NetworkUuid(), inv2.getL3NetworkUuid()), inv1.getL3NetworkUuid(), inv2.getL3NetworkUuid());
        Assert.assertEquals(String.format("different peerAddress[%s, %s]",
                inv1.getPeerAddress(), inv2.getPeerAddress()), inv1.getPeerAddress(), inv2.getPeerAddress());
        Assert.assertEquals(String.format("different authMode[%s, %s]",
                inv1.getAuthMode(), inv2.getAuthMode()), inv1.getAuthMode(), inv2.getAuthMode());
        Assert.assertEquals(String.format("different authKey[%s, %s]",
                inv1.getAuthKey(), inv2.getAuthKey()), inv1.getAuthKey(), inv2.getAuthKey());
        Assert.assertEquals(String.format("different vipUuid[%s, %s]",
                inv1.getVipUuid(), inv2.getVipUuid()), inv1.getVipUuid(), inv2.getVipUuid());
        Assert.assertEquals(String.format("different ikeAuthAlgorithm[%s, %s]",
                inv1.getIkeAuthAlgorithm(), inv2.getIkeAuthAlgorithm()), inv1.getIkeAuthAlgorithm(), inv2.getIkeAuthAlgorithm());
        Assert.assertEquals(String.format("different ikeEncryptionAlgorithm[%s, %s]",
                inv1.getIkeEncryptionAlgorithm(), inv2.getIkeEncryptionAlgorithm()), inv1.getIkeEncryptionAlgorithm(), inv2.getIkeEncryptionAlgorithm());
        Assert.assertEquals(String.format("different ikeDhGroup[%s, %s]",
                inv1.getIkeDhGroup(), inv2.getIkeDhGroup()), inv1.getIkeDhGroup(), inv2.getIkeDhGroup());
        Assert.assertEquals(String.format("different policyAuthAlgorithm[%s, %s]",
                inv1.getPolicyAuthAlgorithm(), inv2.getPolicyAuthAlgorithm()), inv1.getPolicyAuthAlgorithm(), inv2.getPolicyAuthAlgorithm());
        Assert.assertEquals(String.format("different pfs[%s, %s]",
                inv1.getPfs(), inv2.getPfs()), inv1.getPfs(), inv2.getPfs());
        Assert.assertEquals(String.format("different policyMode[%s, %s]",
                inv1.getPolicyMode(), inv2.getPolicyMode()), inv1.getPolicyMode(), inv2.getPolicyMode());
        Assert.assertEquals(String.format("different transformProtocol[%s, %s]",
                inv1.getTransformProtocol(), inv2.getTransformProtocol()), inv1.getTransformProtocol(), inv2.getTransformProtocol());

        if (comparePeerCidrs) {
            List<String> peerCidrs1 = inv1.getPeerCidrSignatures();
            List<String> peerCidrs2 = inv2.getPeerCidrSignatures();
            Assert.assertEquals("different peerCirs amount[%s, %s]", peerCidrs1.size(), peerCidrs2.size());
            for (String c : peerCidrs1) {
                DebugUtils.Assert(peerCidrs2.contains(c), String.format("peer cidr[%s] missing", c));
            }
        }
    }

    private void compare(IPsecConnectionInventory inv1, IPsecInfo inv2) {
        Assert.assertEquals(String.format("different uuid[%s, %s]",
                inv1.getUuid(), inv2.uuid), inv1.getUuid(), inv2.uuid);
        Assert.assertEquals(String.format("different peerAddress[%s, %s]",
                inv1.getPeerAddress(), inv2.peerAddress), inv1.getPeerAddress(), inv2.peerAddress);
        Assert.assertEquals(String.format("different authMode[%s, %s]",
                inv1.getAuthMode(), inv2.authMode), inv1.getAuthMode(), inv2.authMode);
        Assert.assertEquals(String.format("different authKey[%s, %s]",
                inv1.getAuthKey(), inv2.authKey), inv1.getAuthKey(), inv2.authKey);
        VipVO vip = dbf.findByUuid(inv1.getVipUuid(), VipVO.class);
        Assert.assertEquals(String.format("different vip[%s, %s]",
                vip.getIp(), inv2.vip), vip.getIp(), inv2.vip);
        Assert.assertEquals(String.format("different ikeAuthAlgorithm[%s, %s]",
                inv1.getIkeAuthAlgorithm(), inv2.ikeAuthAlgorithm), inv1.getIkeAuthAlgorithm(), inv2.ikeAuthAlgorithm);
        Assert.assertEquals(String.format("different ikeEncryptionAlgorithm[%s, %s]",
                inv1.getIkeEncryptionAlgorithm(), inv2.ikeEncryptionAlgorithm), inv1.getIkeEncryptionAlgorithm(), inv2.ikeEncryptionAlgorithm);
        Assert.assertEquals(String.format("different ikeDhGroup[%s, %s]",
                inv1.getIkeDhGroup(), inv2.ikeDhGroup), inv1.getIkeDhGroup().intValue(), inv2.ikeDhGroup);
        Assert.assertEquals(String.format("different policyAuthAlgorithm[%s, %s]",
                inv1.getPolicyAuthAlgorithm(), inv2.policyAuthAlgorithm), inv1.getPolicyAuthAlgorithm(), inv2.policyAuthAlgorithm);
        Assert.assertEquals(String.format("different pfs[%s, %s]",
                inv1.getPfs(), inv2.pfs), inv1.getPfs(), inv2.pfs);
        Assert.assertEquals(String.format("different policyMode[%s, %s]",
                inv1.getPolicyMode(), inv2.policyMode), inv1.getPolicyMode(), inv2.policyMode);
        Assert.assertEquals(String.format("different transformProtocol[%s, %s]",
                inv1.getTransformProtocol(), inv2.transformProtocol), inv1.getTransformProtocol(), inv2.transformProtocol);
        List<String> peerCidrs1 = inv1.getPeerCidrSignatures();
        Assert.assertEquals("different peerCirs amount[%s, %s]", peerCidrs1.size(), inv2.peerCidrs.size());
        for (String c : peerCidrs1) {
            DebugUtils.Assert(inv2.peerCidrs.contains(c), String.format("peer cidr[%s] missing", c));
        }
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory publicNw = deployer.l3Networks.get("PublicNetwork");
        L3NetworkInventory guestL3 = deployer.l3Networks.get("GuestNetwork");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
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
        inv.setPolicyEncryptionAlgorithm("aes-192");
        inv.setPfs("hs");
        inv.setPolicyMode("tunnel");

        List<String> peerCidrs = asList("10.2.1.0/24", "10.3.1.0/24");
        IPsecConnectionInventory ipsec = api.createIPsecConnection(inv, peerCidrs, null);
        compare(inv, ipsec, false);

        IPsecConnectionVO c = dbf.findByUuid(ipsec.getUuid(), IPsecConnectionVO.class);
        compare(ipsec, IPsecConnectionInventory.valueOf(c), true);

        Assert.assertEquals(1, iconfig.createIPsecConnectionCmdList.size());
        CreateIPsecConnectionCmd cmd = iconfig.createIPsecConnectionCmdList.get(0);
        Assert.assertEquals(1, cmd.infos.size());
        compare(ipsec, cmd.infos.get(0));
    }
}
