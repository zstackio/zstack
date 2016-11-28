package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

/**
 * 1. create 3 vm
 * 2. delete vm1
 * <p>
 * confirm the vm1 changed to Destroyed state
 * <p>
 * 3. recover vm1
 * <p>
 * confirm the vm1 recover to state Stopped
 * <p>
 * 4. attach a l3 to vm1 and start it
 * <p>
 * confirm the vm1 starts successfully
 * <p>
 * 5. destroy vm1
 * 6. update expunge interval and period to 1s
 * <p>
 * confirm the vm1 is deleted
 * <p>
 * 7. update vm deletion policy to Direct
 * 8. delete vm2
 * <p>
 * confirm the vm2 is deleted
 * <p>
 * 9. update vm deletion policy to Never
 * 10. delete vm3
 * <p>
 * confirm vm3 changed to Destroyed state
 * <p>
 * 11. sleep 3s
 * <p>
 * confirm vm3 is still there
 * <p>
 * 12. update volume expunge interval and period to 1s
 * <p>
 * confirm root volume of the vm3 is still there
 * <p>
 * 13. delete and expunge the vm4
 * <p>
 * confirm the vm4 is expunged
 */
public class TestDestroyVm2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestDestroyVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        List<String> l3s = CollectionUtils.transformToList(vm1.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getL3NetworkUuid();
            }
        });

        api.destroyVmInstance(vm1.getUuid());
        for (String l3 : l3s) {
            api.deleteL3Network(l3);
        }

        VmInstanceVO vmvo = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(0, vmvo.getVmNics().size());
    }
}
