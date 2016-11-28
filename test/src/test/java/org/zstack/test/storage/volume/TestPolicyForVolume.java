package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg;
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.ArrayList;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. create a user
 * 2. assign permissions of allow of creating/changing/updating/attaching/detaching/deleting volume to the user
 * <p>
 * confirm the user can do those operations
 * <p>
 * 3. assign permissions of deny of creating/changing/updating/attaching/detaching/deleting volume to the user
 * <p>
 * confirm the user cannot do those operations
 * <p>
 * 4. create a user added in a group
 * 5. assign permissions of allow of creating/changing/updating/attaching/detaching/deleting volume to the group
 * <p>
 * confirm the user can do those operations
 * <p>
 * 6. assign permissions of deny of creating/changing/updating/attaching/detaching/deleting volume to the user
 * <p>
 * confirm the user cannot do those operations
 */
public class TestPolicyForVolume {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/volume/TestPolicyForVolume.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        DiskOfferingInventory dov = deployer.diskOfferings.get("TestRootDiskOffering");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        PrimaryStorageInventory pri = deployer.primaryStorages.get("nfs");
        BackupStorageInventory bs = deployer.backupStorages.get("TestBackupStorage");

        api.stopVmInstance(vm.getUuid());

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        identityCreator.createUser("user1", "password");
        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APICreateDataVolumeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APIChangeVolumeStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APIDeleteDataVolumeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APIUpdateVolumeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APICreateRootVolumeTemplateFromRootVolumeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APICreateDataVolumeFromVolumeTemplateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APICreateDataVolumeTemplateFromVolumeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APIAttachDataVolumeToVmMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APIDetachDataVolumeFromVmMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user1", "allow");
        SessionInventory session = identityCreator.userLogin("user1", "password");

        VolumeInventory root = vm.getRootVolume();
        VolumeInventory vol = api.createDataVolume("data", dov.getUuid(), session);
        api.updateVolume(vol, session);
        api.attachVolumeToVm(vm.getUuid(), vol.getUuid(), session);
        api.detachVolumeFromVm(vol.getUuid(), session);
        ImageInventory dataTemplate = api.addDataVolumeTemplateFromDataVolume(vol.getUuid(), list(bs.getUuid()), session);
        api.createDataVolumeFromTemplate(dataTemplate.getUuid(), pri.getUuid(), session);
        api.createTemplateFromRootVolume("root", root.getUuid(), bs.getUuid(), session);
        api.changeVolumeState(vol.getUuid(), VolumeStateEvent.disable, session);
        api.deleteDataVolume(vol.getUuid(), session);

        vol = api.createDataVolume("data", dov.getUuid(), session);
        identityCreator.detachPolicyFromUser("user1", "allow");

        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APICreateDataVolumeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APIChangeVolumeStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APIDeleteDataVolumeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APIUpdateVolumeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APICreateRootVolumeTemplateFromRootVolumeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APICreateDataVolumeFromVolumeTemplateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APICreateDataVolumeTemplateFromVolumeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APIAttachDataVolumeToVmMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APIDetachDataVolumeFromVmMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean success = false;
        try {
            api.changeVolumeState(vol.getUuid(), VolumeStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updateVolume(vol, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.attachVolumeToVm(vm.getUuid(), vol.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.detachVolumeFromVm(vol.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.addDataVolumeTemplateFromDataVolume(vol.getUuid(), list(bs.getUuid()), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createDataVolumeFromTemplate(dataTemplate.getUuid(), pri.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createTemplateFromRootVolume("root", root.getUuid(), bs.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteDataVolume(vol.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        // user and group
        identityCreator.createUser("user2", "password");
        identityCreator.createGroup("group");
        identityCreator.addUserToGroup("user2", "group");
        identityCreator.attachPolicyToGroup("group", "allow");
        session = identityCreator.userLogin("user2", "password");

        vol = api.createDataVolume("data", dov.getUuid(), session);
        api.updateVolume(vol, session);
        api.attachVolumeToVm(vm.getUuid(), vol.getUuid(), session);
        api.detachVolumeFromVm(vol.getUuid(), session);
        dataTemplate = api.addDataVolumeTemplateFromDataVolume(vol.getUuid(), list(bs.getUuid()), session);
        api.createDataVolumeFromTemplate(dataTemplate.getUuid(), pri.getUuid(), session);
        api.createTemplateFromRootVolume("root", root.getUuid(), bs.getUuid(), session);
        api.changeVolumeState(vol.getUuid(), VolumeStateEvent.disable, session);
        api.deleteDataVolume(vol.getUuid(), session);

        vol = api.createDataVolume("data", dov.getUuid(), session);
        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");

        success = false;
        try {
            api.changeVolumeState(vol.getUuid(), VolumeStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updateVolume(vol, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.attachVolumeToVm(vm.getUuid(), vol.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.detachVolumeFromVm(vol.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.addDataVolumeTemplateFromDataVolume(vol.getUuid(), list(bs.getUuid()), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createDataVolumeFromTemplate(dataTemplate.getUuid(), pri.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createTemplateFromRootVolume("root", root.getUuid(), bs.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteDataVolume(vol.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryVolumeMsg qmsg = new APIQueryVolumeMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qmsg, APIQueryVolumeReply.class, session);
    }
}
