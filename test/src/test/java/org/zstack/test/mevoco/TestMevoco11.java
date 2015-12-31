package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkDnsVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.flat.FlatDhcpBackend.ApplyDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.DhcpInfo;
import org.zstack.network.service.flat.FlatDhcpBackend.PrepareDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.ReleaseDhcpCmd;
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
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;

/**
 * 1. migrate a vm with storage
 * 2. set migration failed
 *
 * confirm the dhcp is set on the dst host and removed from the dst host
 *
 * 3. delete the ip range
 * 4. add a new ip range
 * 5. create a vm
 *
 * confirm the dhcp IP is from the new range
 *
 * 6. add a new IP range2
 * 7. delete the IP range2
 *
 * confirm the DHCP IP tag is still on the L3
 */
public class TestMevoco11 {
    CLogger logger = Utils.getLogger(TestMevoco11.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig kconfig;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    PrimaryStorageOverProvisioningManager ratioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco10.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        ratioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    private void checkNic(VmNicInventory nic, List<DhcpInfo> info) {
        DhcpInfo target = null;
        for (DhcpInfo i : info) {
            if (i.mac.equals(nic.getMac())) {
                target = i;
                break;
            }
        }

        Assert.assertNotNull(target);
        Assert.assertEquals(nic.getIp(), target.ip);
        Assert.assertEquals(nic.getNetmask(), target.netmask);
        Assert.assertEquals(nic.getGateway(), target.gateway);
        Assert.assertEquals(true, target.isDefaultL3Network);
        Assert.assertEquals(VmSystemTags.HOSTNAME.getTokenByResourceUuid(nic.getVmInstanceUuid(), VmSystemTags.HOSTNAME_TOKEN), target.hostname);
        L3NetworkVO l3 = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
        Assert.assertEquals(l3.getDnsDomain(), target.dnsDomain);
        Assert.assertNotNull(target.dns);
        List<String> dns = CollectionUtils.transformToList(l3.getDns(), new Function<String, L3NetworkDnsVO>() {
            @Override
            public String call(L3NetworkDnsVO arg) {
                return arg.getDns();
            }
        });
        Assert.assertTrue(dns.containsAll(target.dns));
        Assert.assertTrue(target.dns.containsAll(dns));
    }
    
	@Test
	public void test() throws ApiSenderException, InterruptedException {
        HostInventory host2 = deployer.hosts.get("host2");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        fconfig.applyDhcpCmdList.clear();
        kconfig.migrateVmSuccess = false;
        boolean s = false;
        try {
            vm = api.migrateVmInstance(vm.getUuid(), host2.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        // dhcp is set on the dst host
        Assert.assertEquals(1, fconfig.applyDhcpCmdList.size());
        ApplyDhcpCmd cmd = fconfig.applyDhcpCmdList.get(0);
        VmNicInventory nic = vm.getVmNics().get(0);
        checkNic(nic, cmd.dhcp);

        // dhcp is removed from the dst host
        Assert.assertEquals(1, fconfig.releaseDhcpCmds.size());
        ReleaseDhcpCmd rcmd = fconfig.releaseDhcpCmds.get(0);
        checkNic(nic, rcmd.dhcp);

        IpRangeInventory ipr = deployer.ipRanges.get("TestIpRange");
        api.deleteIpRange(ipr.getUuid());

        String l3Uuid = ipr.getL3NetworkUuid();
        ipr = new IpRangeInventory();
        ipr.setName("new-ipr");
        ipr.setStartIp("172.16.10.10");
        ipr.setEndIp("172.16.10.200");
        ipr.setGateway("172.16.10.1");
        ipr.setNetmask("255.255.0.0");
        ipr.setL3NetworkUuid(l3Uuid);
        ipr = api.addIpRangeByFullConfig(ipr);

        fconfig.prepareDhcpCmdList.clear();
        api.createVmFromClone(vm);
        Assert.assertEquals(1, fconfig.prepareDhcpCmdList.size());
        PrepareDhcpCmd pcmd = fconfig.prepareDhcpCmdList.get(0);
        Assert.assertNotNull(pcmd.dhcpServerIp);
        Assert.assertTrue(NetworkUtils.isIpv4InRange(pcmd.dhcpServerIp, ipr.getStartIp(), ipr.getEndIp()));
        Assert.assertEquals(ipr.getNetmask(), pcmd.dhcpNetmask);

        ipr = new IpRangeInventory();
        ipr.setName("new-ipr2");
        ipr.setStartIp("192.16.10.10");
        ipr.setEndIp("192.16.10.200");
        ipr.setGateway("192.16.10.1");
        ipr.setNetmask("255.255.0.0");
        ipr.setL3NetworkUuid(l3Uuid);
        ipr = api.addIpRangeByFullConfig(ipr);
        api.deleteIpRange(ipr.getUuid());

        String dhcpTag = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTag(l3Uuid);
        Assert.assertNotNull(dhcpTag);
	}
}
