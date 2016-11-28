package org.zstack.test.network.flat;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.flat.FlatDhcpBackend.ApplyDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.DhcpInfo;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * 1. delete a flat network
 * <p>
 * confirm the namespace is deleted
 */
public class TestFlatNetwork4 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/flatnetwork/TestFlatNetwork4.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.addSpringConfig("flatNetworkServiceSimulator.xml");
        deployer.addSpringConfig("flatNetworkProvider.xml");
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
        api.setTimeout(100000);
        HostInventory host = deployer.hosts.get("host1");
        fconfig.applyDhcpCmdList.clear();
        api.reconnectHost(host.getUuid());

        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        VmNicInventory nic1 = vm1.getVmNics().get(0);

        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        VmNicInventory nic2 = vm2.getVmNics().get(0);

        Assert.assertEquals(2, fconfig.applyDhcpCmdList.size());

        ApplyDhcpCmd cmd = fconfig.applyDhcpCmdList.stream().filter(c -> c.l3NetworkUuid.equals(nic1.getL3NetworkUuid())).findAny().get();
        Assert.assertNotNull(cmd);
        Assert.assertEquals(1, cmd.dhcp.size());
        DhcpInfo dhcp = cmd.dhcp.get(0);
        Assert.assertNotNull(dhcp);
        Assert.assertEquals(nic1.getIp(), dhcp.ip);

        cmd = fconfig.applyDhcpCmdList.stream().filter(c -> c.l3NetworkUuid.equals(nic2.getL3NetworkUuid())).findAny().get();
        Assert.assertNotNull(cmd);
        Assert.assertEquals(1, cmd.dhcp.size());
        dhcp = cmd.dhcp.get(0);
        Assert.assertNotNull(dhcp);
        Assert.assertEquals(nic2.getIp(), dhcp.ip);
    }
}
