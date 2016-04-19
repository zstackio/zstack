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
import org.zstack.network.service.flat.BridgeNameFinder;
import org.zstack.network.service.flat.FlatDhcpBackend.ResetDefaultGatewayCmd;
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
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * 1. create a vm with 2 flat L3 networks
 * 2. delete the vm's default L3 network
 *
 * confirm the ResetDefaultGatewayCmd sent to the backend
 *
 */
public class TestMevocoMultipleNetwork4 {
    CLogger logger = Utils.getLogger(TestMevocoMultipleNetwork4.class);
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
	public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        final L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network1");
        final L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");
        String l31BridgeName = new BridgeNameFinder().findByL3Uuid(l31.getUuid());
        String l32BridgeName = new BridgeNameFinder().findByL3Uuid(l32.getUuid());


        VmNicInventory nic1 = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l31.getUuid()) ? arg : null;
            }
        });
        VmNicInventory nic2 = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l32.getUuid()) ? arg : null;
            }
        });

        api.deleteL3Network(l31.getUuid());
        TimeUnit.SECONDS.sleep(2);
        Assert.assertEquals(1, fconfig.resetDefaultGatewayCmds.size());
        ResetDefaultGatewayCmd cmd = fconfig.resetDefaultGatewayCmds.get(0);
        Assert.assertEquals(nic1.getMac(), cmd.macOfGatewayToRemove);
        Assert.assertEquals(nic1.getGateway(), cmd.gatewayToRemove);
        Assert.assertEquals(l31BridgeName, cmd.bridgeNameOfGatewayToRemove);

        Assert.assertEquals(nic2.getMac(), cmd.macOfGatewayToAdd);
        Assert.assertEquals(nic2.getGateway(), cmd.gatewayToAdd);
        Assert.assertEquals(l32BridgeName, cmd.bridgeNameOfGatewayToAdd);
    }
}
