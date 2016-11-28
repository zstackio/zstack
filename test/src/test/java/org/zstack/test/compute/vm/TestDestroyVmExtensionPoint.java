package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestDestroyVmExtensionPoint {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    VmDestroyExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.addSpringConfig("VmDestroyExtension.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(VmDestroyExtension.class);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory inv = api.listVmInstances(null).get(0);

        ext.setPreventDestroy(true);
        try {
            api.destroyVmInstance(inv.getUuid());
        } catch (ApiSenderException e) {
        }

        ext.setPreventDestroy(false);
        ext.setExpectedUuid(inv.getUuid());
        api.destroyVmInstance(inv.getUuid());
        Assert.assertTrue(ext.isBeforeCalled());
        Assert.assertTrue(ext.isAfterCalled());
    }
}
