package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestVmHostname {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        api.setHostname(vm.getUuid(), "vm1", null);
        String hostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN);
        Assert.assertEquals("vm1", hostname);

        String hostname1 = api.getHostname(vm.getUuid(), null);
        Assert.assertEquals(hostname1, hostname);

        api.setHostname(vm.getUuid(), "vm2", null);
        hostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN);
        Assert.assertEquals("vm2", hostname);

        api.deleteHostname(vm.getUuid(), null);
        Assert.assertFalse(VmSystemTags.HOSTNAME.hasTag(vm.getUuid()));
        hostname1 = api.getHostname(vm.getUuid(), null);
        Assert.assertNull(hostname1);

        api.deleteHostname(vm.getUuid(), null);
        Assert.assertFalse(VmSystemTags.HOSTNAME.hasTag(vm.getUuid()));

        // invalid hostname
        boolean s = false;
        try {
            api.setHostname(vm.getUuid(), "vm2::2", null);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        // duplicate hostnames
        VmInstanceInventory vm2 = api.createVmFromClone(vm);
        api.setHostname(vm2.getUuid(), "vm2", null);
        s = false;
        try {
            api.setHostname(vm.getUuid(), "vm2", null);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
    }
}
