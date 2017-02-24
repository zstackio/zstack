package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.ImageEO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.vm.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.stream.Collectors;

/**
 * 1. a vm with a single L3
 * 2. destroy the vm
 * 3. delete the l3
 * 4. recover the vm
 * 5  start the vm
 * <p>
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

        // The code below is to test the MySQL trigger for ImageEO
        ImageEO eo;

        api.deleteImage(vm1.getImageUuid());
        api.expungeImage(vm1.getImageUuid(),
                deployer.backupStorages.values().stream()
                        .map(BackupStorageInventory::getUuid)
                        .collect(Collectors.toList()),
                null);
        eo = dbf.findByUuid(vm1.getImageUuid(), ImageEO.class);
        Assert.assertNotNull("ImageEO should still exist", eo);
        Assert.assertNotNull("ImageEO should be marked as deleted", eo.getDeleted());
        Assert.assertFalse("ImageVO should have been deleted", dbf.isExist(eo.getUuid(), ImageVO.class));

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString());
        api.destroyVmInstance(vm1.getUuid());

        eo = dbf.findByUuid(vm1.getImageUuid(), ImageEO.class);
        Assert.assertNull("ImageEO should have been cleaned up by trigger", eo);
        Assert.assertFalse("VmInstanceEO should have been cleaned", dbf.isExist(vm1.getUuid(), VmInstanceEO.class));
    }
}
