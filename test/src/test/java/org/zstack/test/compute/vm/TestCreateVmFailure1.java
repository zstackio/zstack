package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.CreateVmOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.aop.CloudBusAopProxy;
import org.zstack.test.deployer.Deployer;

/**
 * 1. create VM with disk offering
 * 2. make creation fail
 * <p>
 * confirm the error returns quickly instead of timeout
 */
public class TestCreateVmFailure1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    CloudBusAopProxy busProxy;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.addSpringConfig("CloudBusAopProxy.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        busProxy = loader.getComponent(CloudBusAopProxy.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        busProxy.addMessage(CreateVmOnHypervisorMsg.class, CloudBusAopProxy.Behavior.FAIL);
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        boolean s = false;
        try {
            api.createVmFromClone(vm);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
    }

}
