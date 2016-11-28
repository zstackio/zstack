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

public class TestStartVmExtensionPoint {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    VmStartExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.addSpringConfig("VmStartExtension.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(VmStartExtension.class);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory inv = api.listVmInstances(null).get(0);
        api.stopVmInstance(inv.getUuid());

        ext.setPreventStart(true);
        try {
            api.startVmInstance(inv.getUuid());
        } catch (ApiSenderException e) {
        }

        ext.setPreventStart(false);
        ext.setExpectedUuid(inv.getUuid());
        api.startVmInstance(inv.getUuid());
        Assert.assertTrue(ext.isBeforeCalled());
        Assert.assertTrue(ext.isAfterCalled());
    }
}
