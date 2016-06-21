package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.service.*;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.flat.FlatDhcpBackend.ApplyDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.DhcpInfo;
import org.zstack.network.service.flat.FlatDhcpBackend.PrepareDhcpCmd;
import org.zstack.network.service.flat.FlatNetworkServiceConstant;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.network.service.flat.FlatNetworkSystemTags;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. create a vm with virtual router
 * 2. detach the virtual router services from the L3
 * 3. attach the flat network services to the L3
 * 4. reconnect the host
 *
 * confirm the IPs are set on the vm by the flat network services
 *
 */
public class TestMevoco20 {
    CLogger logger = Utils.getLogger(TestMevoco20.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    HostCapacityOverProvisioningManager hostRatioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco20.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);
        hostRatioMgr = loader.getComponent(HostCapacityOverProvisioningManager.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        Map<String, List<String>> services = new HashMap<String, List<String>>();
        for (NetworkServiceL3NetworkRefInventory ref : l3.getNetworkServices()) {
            List<String> types = services.get(ref.getNetworkServiceProviderUuid());
            if (types == null) {
                types = new ArrayList<String>();
                services.put(ref.getNetworkServiceProviderUuid(), types);
            }
            types.add(ref.getNetworkServiceType());
        }

        api.detachNetworkServicesFromL3Network(l3.getUuid(), services);

        SimpleQuery<NetworkServiceProviderVO> q = dbf.createQuery(NetworkServiceProviderVO.class);
        q.add(NetworkServiceProviderVO_.type, SimpleQuery.Op.EQ, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        NetworkServiceProviderVO vo = q.find();

        api.attachNetworkServiceToL3Network(l3.getUuid(), vo.getUuid(), list(NetworkServiceType.DHCP.toString()));

        fconfig.applyDhcpCmdList.clear();

        fconfig.connectCmds.clear();
        HostInventory host = deployer.hosts.get("host1");
        api.reconnectHost(host.getUuid());

        VmNicInventory nic = vm.getVmNics().get(0);
        ApplyDhcpCmd acmd = fconfig.applyDhcpCmdList.get(0);
        Assert.assertFalse(acmd.dhcp.isEmpty());
        DhcpInfo dhcp = acmd.dhcp.get(0);
        Assert.assertEquals(nic.getIp(), dhcp.ip);
        Assert.assertEquals(nic.getMac(), dhcp.mac);
        Assert.assertEquals(nic.getGateway(), dhcp.gateway);
        Assert.assertEquals(nic.getNetmask(), dhcp.netmask);
        Assert.assertTrue(dhcp.isDefaultL3Network);
        Assert.assertNotNull(dhcp.dns);
        Assert.assertTrue(dhcp.dns.contains("1.1.1.1"));
        Assert.assertNotNull(dhcp.bridgeName);
        Assert.assertNotNull(dhcp.namespaceName);

        Assert.assertTrue(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.hasTag(l3.getUuid()));
        Map<String, String> tokens = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokensByResourceUuid(l3.getUuid());
        String dhcpServerIp = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN);
        String dhcpServerIpUuid = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN);
        UsedIpVO ipvo = dbf.findByUuid(dhcpServerIpUuid, UsedIpVO.class);
        Assert.assertNotNull(ipvo);
        Assert.assertFalse(fconfig.prepareDhcpCmdList.isEmpty());
        PrepareDhcpCmd cmd = fconfig.prepareDhcpCmdList.get(0);
        Assert.assertEquals(ipvo.getIp(), dhcpServerIp);
        Assert.assertEquals(dhcpServerIp, cmd.dhcpServerIp);
        Assert.assertEquals(dhcp.bridgeName, cmd.bridgeName);
        Assert.assertEquals(dhcp.namespaceName, cmd.namespaceName);
        Assert.assertEquals(ipvo.getNetmask(), cmd.dhcpNetmask);

        Assert.assertEquals(1, fconfig.connectCmds.size());
    }
}
