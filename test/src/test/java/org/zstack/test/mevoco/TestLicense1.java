package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigInventory;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.license.LicenseErrors;
import org.zstack.license.LicenseInfo;
import org.zstack.license.LicenseType;
import org.zstack.mevoco.MevocoGlobalConfig;
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
import org.zstack.utils.logging.CLogger;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class TestLicense1 {
    CLogger logger = Utils.getLogger(TestLicense1.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    MockLicenseManagerImpl licMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        LicenseInfo licInfo = new LicenseInfo();
        licInfo.setLicenseType(LicenseType.Trial);
        MockLicenseManagerImpl.mockLicenseInfo = licInfo;

        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        licMgr = loader.getComponent(MockLicenseManagerImpl.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        ClusterInventory cluster = deployer.clusters.get("Cluster1");
        HostInventory host1 = deployer.hosts.get("host1");
        boolean s = false;
        try {
            // trail license can have 1 host only
            api.addKvmHost("host2", "127.0.0.1", cluster.getUuid());
        } catch (ApiSenderException e) {
            if (LicenseErrors.LICENSE_NOT_PERMITTED.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);

        // can add host with prepaid license
        LicenseInfo licInfo = new LicenseInfo();
        licInfo.setLicenseType(LicenseType.Paid);
        licInfo.setHostNum(2);
        MockLicenseManagerImpl.mockLicenseInfo = licInfo;
        KVMHostInventory host = api.addKvmHost("host2", "127.0.0.1", cluster.getUuid());
        api.deleteHost(host.getUuid());

        // can add host with prepaid license
        licInfo = new LicenseInfo();
        licInfo.setLicenseType(LicenseType.OEM);
        licInfo.setHostNum(2);
        MockLicenseManagerImpl.mockLicenseInfo = licInfo;
        host = api.addKvmHost("host2", "127.0.0.1", cluster.getUuid());
        api.deleteHost(host.getUuid());

        // no operation with expired license
        licInfo = new LicenseInfo();
        licInfo.setLicenseType(LicenseType.Expired);
        MockLicenseManagerImpl.mockLicenseInfo = licInfo;
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        s = false;
        try {
            api.stopVmInstance(vm.getUuid());
        } catch (ApiSenderException e) {
            if (LicenseErrors.LICENSE_EXPIRED.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);

        // no premium api for free license
        licInfo = new LicenseInfo();
        licInfo.setLicenseType(LicenseType.Free);
        MockLicenseManagerImpl.mockLicenseInfo = licInfo;


        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        s = false;
        try {
            api.createSystemTag(ioinv.getUuid(), MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.instantiateTag(
                    map(e(MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN, 10000))
            ), InstanceOfferingVO.class);
        } catch (ApiSenderException e) {
            if (LicenseErrors.LICENSE_NOT_PERMITTED.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);

        s = false;
        try {
            api.createSystemTag(ioinv.getUuid(), MevocoSystemTags.VOLUME_TOTAL_BANDWIDTH.instantiateTag(
                    map(e(MevocoSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN, 10000))
            ), InstanceOfferingVO.class);
        } catch (ApiSenderException e) {
            if (LicenseErrors.LICENSE_NOT_PERMITTED.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);

        s = false;
        try {
            api.createSystemTag(ioinv.getUuid(), MevocoSystemTags.VOLUME_TOTAL_IOPS.instantiateTag(
                    map(e(MevocoSystemTags.VOLUME_TOTAL_IOPS_TOKEN, 10000))
            ), InstanceOfferingVO.class);
        } catch (ApiSenderException e) {
            if (LicenseErrors.LICENSE_NOT_PERMITTED.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);

        s = false;
        GlobalConfigInventory ginv = new GlobalConfigInventory();
        ginv.setCategory(MevocoGlobalConfig.CATEGORY);
        ginv.setName(MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.getName());
        ginv.setValue("1.5");
        try {
            api.updateGlobalConfig(ginv);
        } catch (ApiSenderException e) {
            if (LicenseErrors.LICENSE_NOT_PERMITTED.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);

        s = false;
        ginv = new GlobalConfigInventory();
        ginv.setCategory(MevocoGlobalConfig.CATEGORY);
        ginv.setName(MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.getName());
        ginv.setValue("1.5");
        try {
            api.updateGlobalConfig(ginv);
        } catch (ApiSenderException e) {
            if (LicenseErrors.LICENSE_NOT_PERMITTED.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);

        Map<String, String> caps = api.getLicenseCapabilities();
        // free license has no limit on host adding
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("addHost"));
        Assert.assertEquals(Boolean.FALSE.toString(), caps.get("qos"));
        Assert.assertEquals(Boolean.FALSE.toString(), caps.get("monitoring"));
        Assert.assertEquals(Boolean.FALSE.toString(), caps.get("overProvisioning"));

        // oem license with 1 host, the addHost capability should be false
        licInfo = new LicenseInfo();
        licInfo.setLicenseType(LicenseType.OEM);
        licInfo.setHostNum(1);
        MockLicenseManagerImpl.mockLicenseInfo = licInfo;
        caps = api.getLicenseCapabilities();
        Assert.assertEquals(Boolean.FALSE.toString(), caps.get("addHost"));
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("qos"));
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("monitoring"));
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("overProvisioning"));

        // prepaid license with 10 host, the addHost capability should be true
        licInfo = new LicenseInfo();
        licInfo.setLicenseType(LicenseType.Paid);
        licInfo.setHostNum(10);
        MockLicenseManagerImpl.mockLicenseInfo = licInfo;
        caps = api.getLicenseCapabilities();
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("addHost"));
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("qos"));
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("monitoring"));
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("overProvisioning"));

        // expired license with 10 host, the addHost capability should be false
        licInfo = new LicenseInfo();
        licInfo.setLicenseType(LicenseType.Expired);
        licInfo.setHostNum(10);
        MockLicenseManagerImpl.mockLicenseInfo = licInfo;
        caps = api.getLicenseCapabilities();
        Assert.assertEquals(Boolean.FALSE.toString(), caps.get("addHost"));
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("qos"));
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("monitoring"));
        Assert.assertEquals(Boolean.TRUE.toString(), caps.get("overProvisioning"));
    }
}
