package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.network.service.flat.FlatNetworkSystemTags;
import org.zstack.network.service.flat.FlatUserdataBackend.ApplyUserdataCmd;
import org.zstack.network.service.flat.FlatUserdataBackend.ReleaseUserdataCmd;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.*;

/**
 * 1. create a vm with user data
 *
 * confirm the user data set
 *
 * 2. stop the vm
 *
 * confirm the user data removed
 *
 * 3. start the vm
 *
 * confirm the user data set
 *
 * 4. stop the vm
 * 5. delete the userdata
 * 6. start the vm
 *
 * confirm the metadata set
 *
 * 7. create another vm with user data
 * 8. delete the vm
 *
 * confirm the user data removed
 */
public class TestMevoco4 {
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
        deployer = new Deployer("deployerXml/mevoco/TestMevoco.xml", con);
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
        fconfig.applyUserdataCmds.clear();

        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("small");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        VmCreator creator = new VmCreator(api);
        creator.imageUuid = img.getUuid();
        creator.session = api.getAdminSession();
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.name = "vm";
        creator.systemTags.add(VmSystemTags.USERDATA.instantiateTag(map(e(VmSystemTags.USERDATA_TOKEN, userdata))));
        creator.addL3Network(l3.getUuid());
        VmInstanceInventory vm = creator.create();

        L2NetworkInventory l2 = deployer.l2Networks.get("TestL2Network");
        String bridgeName = KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2.getUuid(), KVMSystemTags.L2_BRIDGE_NAME_TOKEN);

        VmNicInventory nic = vm.getVmNics().get(0);
        Map<String, String> tokens = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokensByResourceUuid(l3.getUuid());
        String dhcpServerIp = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN);
        Assert.assertFalse(fconfig.applyUserdataCmds.isEmpty());
        ApplyUserdataCmd cmd = fconfig.applyUserdataCmds.get(0);
        Assert.assertEquals(vm.getUuid(), cmd.userdata.metadata.vmUuid);
        Assert.assertEquals(dhcpServerIp, cmd.userdata.dhcpServerIp);
        Assert.assertEquals(nic.getIp(), cmd.userdata.vmIp);
        Assert.assertEquals(userdata, cmd.userdata.userdata);
        Assert.assertEquals(bridgeName, cmd.userdata.bridgeName);

        vm = api.stopVmInstance(vm.getUuid());
        Assert.assertFalse(fconfig.releaseUserdataCmds.isEmpty());
        ReleaseUserdataCmd rcmd = fconfig.releaseUserdataCmds.get(0);
        Assert.assertEquals(bridgeName, rcmd.bridgeName);
        Assert.assertEquals(nic.getIp(), rcmd.vmIp);

        fconfig.applyUserdataCmds.clear();
        vm = api.startVmInstance(vm.getUuid());
        Assert.assertFalse(fconfig.applyUserdataCmds.isEmpty());
        cmd = fconfig.applyUserdataCmds.get(0);
        Assert.assertEquals(vm.getUuid(), cmd.userdata.metadata.vmUuid);
        Assert.assertEquals(dhcpServerIp, cmd.userdata.dhcpServerIp);
        Assert.assertEquals(nic.getIp(), cmd.userdata.vmIp);
        Assert.assertEquals(userdata, cmd.userdata.userdata);
        Assert.assertEquals(bridgeName, cmd.userdata.bridgeName);

        vm = api.stopVmInstance(vm.getUuid());

        fconfig.applyUserdataCmds.clear();
        VmSystemTags.USERDATA.delete(vm.getUuid());
        api.startVmInstance(vm.getUuid());
        Assert.assertFalse(fconfig.applyUserdataCmds.isEmpty());

        vm = creator.create();
        nic = vm.getVmNics().get(0);
        fconfig.releaseUserdataCmds.clear();
        api.destroyVmInstance(vm.getUuid());
        Assert.assertFalse(fconfig.releaseUserdataCmds.isEmpty());
        rcmd = fconfig.releaseUserdataCmds.get(0);
        Assert.assertEquals(bridgeName, rcmd.bridgeName);
        Assert.assertEquals(nic.getIp(), rcmd.vmIp);

        String sshkey = "ssh-rsa xxx";
        String rootPassword = "rootPassword";
        creator.systemTags = list(
                VmSystemTags.SSHKEY.instantiateTag(map(e(VmSystemTags.SSHKEY_TOKEN, sshkey))),
                VmSystemTags.ROOT_PASSWORD.instantiateTag(map(e(VmSystemTags.ROOT_PASSWORD_TOKEN, rootPassword)))
        );
        fconfig.applyUserdataCmds.clear();
        vm = creator.create();
        cmd = fconfig.applyUserdataCmds.get(0);
        Assert.assertTrue(cmd.userdata.userdata.contains(sshkey));
        Assert.assertTrue(cmd.userdata.userdata.contains(rootPassword));

        creator.systemTags = list(
                VmSystemTags.USERDATA.instantiateTag(map(e(VmSystemTags.USERDATA_TOKEN, "xxx"))),
                VmSystemTags.SSHKEY.instantiateTag(map(e(VmSystemTags.SSHKEY_TOKEN, sshkey))),
                VmSystemTags.ROOT_PASSWORD.instantiateTag(map(e(VmSystemTags.ROOT_PASSWORD_TOKEN, rootPassword)))
        );
        fconfig.applyUserdataCmds.clear();
        vm = creator.create();
        cmd = fconfig.applyUserdataCmds.get(0);
        Assert.assertFalse(cmd.userdata.userdata.contains(sshkey));
        Assert.assertFalse(cmd.userdata.userdata.contains(rootPassword));
    }
}
