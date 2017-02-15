package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.network.l3.APIGetIpAddressCapacityReply;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.volume.VolumeGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 * Created by zouye on 2017/2/13.
 */
public class TestDeleteCreatedVm {
    /**
     * 1. create vm but not start
     * 2. delete vm
     * <p>
     * confirm the vm changed to Destroyed state
     */
    public class TestDestroyVm {
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
            VmInstanceInventory vm = deployer.vms.get("TestVm");
            vm.setState(VmInstanceState.Created.toString());

            APIGetIpAddressCapacityReply ipcount = api.getIpAddressCapacityByAll();
            api.destroyVmInstance(vm.getUuid());
            VmInstanceVO vmvo1 = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
            Assert.assertNotNull(vmvo1);
            Assert.assertEquals(VmInstanceState.Created, vmvo1.getState());
            vm = VmInstanceInventory.valueOf(vmvo1);
            Assert.assertNotNull(vm.getRootVolume());
            APIGetIpAddressCapacityReply ipcount1 = api.getIpAddressCapacityByAll();

            Assert.assertEquals(ipcount.getAvailableCapacity() + vm.getVmNics().size(), ipcount1.getAvailableCapacity());

            for (VmNicVO nic : vmvo1.getVmNics()) {
                Assert.assertNull(nic.getUsedIpUuid());
                Assert.assertNull(nic.getGateway());
                Assert.assertNull(nic.getIp());
                Assert.assertNull(nic.getNetmask());
            }

            api.destroyVmInstance(vm.getUuid());
            VmGlobalConfig.VM_EXPUNGE_PERIOD.updateValue(1);
            VmGlobalConfig.VM_EXPUNGE_INTERVAL.updateValue(1);

            TimeUnit.SECONDS.sleep(3);
            vmvo1 = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
            Assert.assertTrue(vmvo1 == null);
        }
    }
}
