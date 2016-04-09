package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

/**
 * 1. a vm with a single L3
 * 2. destroy the vm
 * 3. delete the l3
 * 4. recover the vm
 * 5  start the vm
 *
 * confirm unable to start the vm and the vm state is stopped
 */
public class TestDestroyVm1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestDestroyVm1.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");

        api.destroyVmInstance(vm1.getUuid());
        api.deleteL3Network(l3.getUuid());
        api.recoverVm(vm1.getUuid(), null);
        try {
            api.startVmInstance(vm1.getUuid());
        } catch (ApiSenderException e) {
            //pass
        }

        VmInstanceVO vmvo = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Stopped, vmvo.getState());
    }
}
