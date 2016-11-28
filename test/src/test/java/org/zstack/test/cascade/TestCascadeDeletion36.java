package org.zstack.test.cascade;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

/**
 * 1. delete image
 * 2. stop vm
 * 3. start vm
 * 4. reboot vm
 * 5. destroy vm
 * <p>
 * confirm all operations success
 */
public class TestCascadeDeletion36 {
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
        ImageInventory img = deployer.images.get("TestImage");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        api.deleteImage(img.getUuid());

        api.stopVmInstance(vm.getUuid());
        api.startVmInstance(vm.getUuid());
        api.rebootVmInstance(vm.getUuid());
        api.destroyVmInstance(vm.getUuid());
    }
}
