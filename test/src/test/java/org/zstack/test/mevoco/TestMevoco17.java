package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.mevoco.KVMAddOns.NicQos;
import org.zstack.mevoco.KVMAddOns.VolumeQos;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
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

import java.util.Map;

/**
 * 1. create a vm with qos
 * 2. change vm's instance offering to another one with different qos
 * 3. stop/start vm
 * confirm the vm's qos setting changed
 *
 * 4. change vm's instance offering to one without qos
 * 5. stop/start vm
 *
 * confirm ths vm's qos is gone
 *
 *
 */
public class TestMevoco17 {
    CLogger logger = Utils.getLogger(TestMevoco17.class);
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
        deployer = new Deployer("deployerXml/mevoco/TestMevoco17.xml", con);
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
        InstanceOfferingInventory offering2 = deployer.instanceOfferings.get("TestInstanceOffering1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());
        api.changeInstanceOffering(vm.getUuid(), offering2.getUuid());
        vm = api.startVmInstance(vm.getUuid());

        long networkBandwidth = Long.valueOf(MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.getTokenByResourceUuid(offering2.getUuid(), MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN));
        long ioBandwidth = Long.valueOf(MevocoSystemTags.VOLUME_TOTAL_BANDWIDTH.getTokenByResourceUuid(offering2.getUuid(), MevocoSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN));
        long iops = Long.valueOf(MevocoSystemTags.VOLUME_TOTAL_IOPS.getTokenByResourceUuid(offering2.getUuid(), MevocoSystemTags.VOLUME_TOTAL_IOPS_TOKEN));

        VmNicInventory nic = vm.getVmNics().get(0);
        VolumeInventory root = vm.getRootVolume();

        StartVmCmd scmd = kconfig.startVmCmd;
        Assert.assertNotNull(scmd);
        Map m = (Map) scmd.getAddons().get(MevocoConstants.KVM_NIC_QOS);
        Assert.assertNotNull(m);
        NicQos nqos = JSONObjectUtil.rehashObject(m.get(nic.getUuid()), NicQos.class);
        Assert.assertEquals(networkBandwidth, (long)nqos.outboundBandwidth);

        m = (Map) scmd.getAddons().get(MevocoConstants.KVM_VOLUME_QOS);
        VolumeQos vqos = JSONObjectUtil.rehashObject(m.get(root.getUuid()), VolumeQos.class);
        Assert.assertEquals(ioBandwidth, (long)vqos.totalBandwidth);
        Assert.assertEquals(iops, (long)vqos.totalIops);

        InstanceOfferingInventory offering3 = deployer.instanceOfferings.get("small");
        api.stopVmInstance(vm.getUuid());
        api.changeInstanceOffering(vm.getUuid(), offering3.getUuid());
        vm = api.startVmInstance(vm.getUuid());

        Assert.assertFalse(MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.hasTag(vm.getUuid(), VmInstanceVO.class));
        Assert.assertFalse(MevocoSystemTags.VOLUME_TOTAL_IOPS.hasTag(vm.getUuid(), VmInstanceVO.class));
        Assert.assertFalse(MevocoSystemTags.VOLUME_TOTAL_BANDWIDTH.hasTag(vm.getUuid(), VmInstanceVO.class));

        scmd = kconfig.startVmCmd;
        Assert.assertNotNull(scmd);
        m = (Map) scmd.getAddons().get(MevocoConstants.KVM_NIC_QOS);
        Assert.assertNull(m);

        m = (Map) scmd.getAddons().get(MevocoConstants.KVM_VOLUME_QOS);
        Assert.assertNull(m);
    }
}
