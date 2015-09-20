package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOffering;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.mevoco.KVMAddOns.NicQos;
import org.zstack.mevoco.KVMAddOns.VolumeQos;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.network.service.flat.FlatDhcpBackend.ApplyDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.DhcpInfo;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * 1. create a vm with mevoco setting
 *
 * confirm the vm created successfully
 * confirm the flat dhcp works
 * confirm the over-provisioning works
 */
public class TestMevoco {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("mevoco.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("flatNetworkServiceSimulator.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);
        VolumeInventory root = vm.getRootVolume();

        StartVmCmd scmd = kconfig.startVmCmd;
        Assert.assertNotNull(scmd);
        Map m = (Map) scmd.getAddons().get(MevocoConstants.KVM_NIC_QOS);
        Assert.assertNotNull(m);
        NicQos nqos = JSONObjectUtil.rehashObject(m.get(nic.getUuid()), NicQos.class);
        Assert.assertEquals(Long.valueOf(1000), nqos.outboundBandwidth);

        m = (Map) scmd.getAddons().get(MevocoConstants.KVM_VOLUME_QOS);
        VolumeQos vqos = JSONObjectUtil.rehashObject(m.get(root.getUuid()), VolumeQos.class);
        Assert.assertEquals(Long.valueOf(2000), vqos.totalBandwidth);
        Assert.assertEquals(Long.valueOf(10000), vqos.totalIops);

        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        long networkBandwidth = Long.valueOf(MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.getTokenByResourceUuid(ioinv.getUuid(), MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN));
        Assert.assertEquals(1000, networkBandwidth);
        long ioBandwidth = Long.valueOf(MevocoSystemTags.VOLUME_TOTAL_BANDWIDTH.getTokenByResourceUuid(ioinv.getUuid(), MevocoSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN));
        Assert.assertEquals(2000, ioBandwidth);
        long iops = Long.valueOf(MevocoSystemTags.VOLUME_TOTAL_IOPS.getTokenByResourceUuid(ioinv.getUuid(), MevocoSystemTags.VOLUME_TOTAL_IOPS_TOKEN));
        Assert.assertEquals(10000, iops);

        Assert.assertFalse(fconfig.applyDhcpCmdList.isEmpty());
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

        HostInventory host = deployer.hosts.get("host1");
        HostVO hostVO = dbf.findByUuid(host.getUuid(), HostVO.class);

        long usedMem = hostVO.getCapacity().getTotalMemory() - hostVO.getCapacity().getAvailableMemory();
        Assert.assertEquals(usedMem, (long) (ioinv.getMemorySize() / MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.value(Float.class)));

        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageVO localVO = dbf.findByUuid(local.getUuid(), PrimaryStorageVO.class);
        long usedDisk = localVO.getCapacity().getTotalCapacity() - localVO.getCapacity().getAvailableCapacity();
        VolumeInventory vol = vm.getRootVolume();
        Assert.assertEquals(usedDisk, (long)(vol.getSize() / MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.value(Float.class)));
    }
}
