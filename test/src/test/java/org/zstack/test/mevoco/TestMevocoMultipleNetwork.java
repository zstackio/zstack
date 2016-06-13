package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.flat.FlatDhcpBackend.ApplyDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.DhcpInfo;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
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
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

/**
 * 1. create a vm with mevoco setting
 *
 * confirm the vm created successfully
 * confirm the qos set
 * confirm the flat dhcp works
 * confirm the over-provisioning works
 *
 * 2. attach a data volume
 * confirm the qos set
 *
 * 3. add DNS
 * confirm the DNS added successfully
 */
public class TestMevocoMultipleNetwork {
    CLogger logger = Utils.getLogger(TestMevocoMultipleNetwork.class);
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
        deployer = new Deployer("deployerXml/mevoco/TestMevocoMultipleNetwork.xml", con);
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
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        final L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network1");
        final L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");

        VmNicInventory n = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l31.getUuid()) ? arg : null;
            }
        });
        Assert.assertEquals(0, n.getDeviceId());

        n = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l32.getUuid()) ? arg : null;
            }
        });
        Assert.assertEquals(1, n.getDeviceId());

        goOn:
        for (ApplyDhcpCmd cmd : fconfig.applyDhcpCmdList) {
            DhcpInfo info = cmd.dhcp.get(0);
            if (!info.isDefaultL3Network && info.hostname != null) {
                Assert.fail(String.format("wrong hostname set. %s", JSONObjectUtil.toJsonString(info)));
            }

            for (VmNicInventory nic : vm.getVmNics()) {
                if (info.ip.equals(nic.getIp()) && info.gateway.equals(nic.getGateway()) && info.netmask.equals(nic.getNetmask())) {
                    break goOn;
                }
            }

            Assert.fail(String.format("nic %s", JSONObjectUtil.toJsonString(cmd.dhcp)));
        }
    }
}
