package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.AttachDataVolumeCmd;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.mevoco.KVMAddOns.NicQos;
import org.zstack.mevoco.KVMAddOns.VolumeQos;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.network.service.flat.FlatDhcpBackend.ApplyDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.DhcpInfo;
import org.zstack.network.service.flat.FlatDhcpBackend.PrepareDhcpCmd;
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
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform._;

/**
 * 1. create a vm with flat network
 * 2. delete the l3
 *
 * confirm the DHCP server IP released
 *
 * 3. attach a new L3
 *
 * confirm the DHCP server IP set
 *
 * 4. delete the ip range
 *
 * confirm the DHCP server IP released
 *
 * 5. add a new ip range
 *
 * confirm the DHCP server IP set
 *
 */
public class TestMevoco21 {
    CLogger logger = Utils.getLogger(TestMevoco21.class);
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
        deployer = new Deployer("deployerXml/mevoco/TestMevoco21.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
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
        L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.deleteL3Network(l31.getUuid());
        Assert.assertFalse(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.hasTag(l31.getUuid()));

        api.attachNic(vm.getUuid(), l32.getUuid());
        api.stopVmInstance(vm.getUuid());
        vm = api.startVmInstance(vm.getUuid());
        Assert.assertTrue(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.hasTag(l32.getUuid()));
        Assert.assertEquals(1, vm.getVmNics().size());
        VmNicInventory nic = vm.getVmNics().get(0);
        Assert.assertEquals(l32.getUuid(), nic.getL3NetworkUuid());

        IpRangeInventory ipr = deployer.ipRanges.get("TestIpRange2");
        api.deleteIpRange(ipr.getUuid());
        Assert.assertFalse(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.hasTag(l31.getUuid()));

        ipr = api.addIpRange(l32.getUuid(), "172.168.0.200", "172.168.0.220", "172.168.0.1", "255.255.255.0");
        api.stopVmInstance(vm.getUuid());
        vm = api.startVmInstance(vm.getUuid());
        Assert.assertTrue(NetworkUtils.isIpv4InRange(vm.getVmNics().get(0).getIp(), ipr.getStartIp(), ipr.getEndIp()));
        Assert.assertTrue(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.hasTag(l32.getUuid()));
    }
}
