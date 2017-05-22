package org.zstack.test.userdata;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.flat.BridgeNameFinder;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.network.service.flat.FlatNetworkSystemTags;
import org.zstack.network.service.flat.FlatUserdataBackend.BatchApplyUserdataCmd;
import org.zstack.network.service.flat.FlatUserdataBackend.UserdataTO;
import org.zstack.network.service.userdata.UserdataGlobalProperty;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.Base64;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. create a vm with user data
 * 2. reconnect the vm's host
 * <p>
 * confirm the userdata synced on the host
 */
public class TestUserdata2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);
    String userdata = "hello, world";

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/userdata/TestUserdata2.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
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

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ImageInventory img = deployer.images.get("TestImage");

        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("small");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        VmCreator creator = new VmCreator(api);
        creator.imageUuid = img.getUuid();
        creator.session = api.getAdminSession();
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.name = "vm";
        creator.systemTags.add(VmSystemTags.USERDATA.instantiateTag(map(e(VmSystemTags.USERDATA_TOKEN,
                new String(Base64.getEncoder().encode(userdata.getBytes()))))));
        creator.addL3Network(l3.getUuid());
        VmInstanceInventory vm = creator.create();

        VmNicInventory nic = vm.getVmNics().get(0);

        api.reconnectHost(vm.getHostUuid());
        Assert.assertEquals(1, fconfig.batchApplyUserdataCmds.size());
        BatchApplyUserdataCmd cmd = fconfig.batchApplyUserdataCmds.get(0);
        Assert.assertEquals(1, cmd.userdata.size());
        UserdataTO to = cmd.userdata.get(0);

        Assert.assertEquals(userdata, to.userdata);
        Assert.assertEquals(vm.getUuid(), to.metadata.vmUuid);
        Assert.assertEquals(nic.getIp(), to.vmIp);
        String brName = new BridgeNameFinder().findByL3Uuid(nic.getL3NetworkUuid());
        Assert.assertEquals(brName, to.bridgeName);
        String dhcpIp = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokenByResourceUuid(nic.getL3NetworkUuid(), FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN);
        Assert.assertEquals(dhcpIp, to.dhcpServerIp);
        Assert.assertEquals(UserdataGlobalProperty.HOST_PORT, to.port);
    }
}
