package org.zstack.test.storage.snapshot;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotMsg;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg;
import org.zstack.header.volume.APICreateVolumeSnapshotMsg;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;


/**
 * 1. create a user
 * 2. assign permissions of allow of creating/updating/changing/reverting/deleting snapshots to the user
 * <p>
 * confirm the user can do those operations
 * <p>
 * 3. assign permissions of deny of creating/updating/changing/reverting/deleting snapshots to the user
 * <p>
 * confirm the user cannot do those operations
 * <p>
 * 4. create a user added in a group
 * 5. assign permissions of allow of creating/updating/changing/reverting/deleting snapshots to the group
 * <p>
 * confirm the user can do those operations
 * <p>
 * 6. assign permissions of deny of creating/updating/changing/reverting/deleting snapshots to the group
 * <p>
 * confirm the user cannot do those operations
 */
public class TestPolicyForSnapshot {
    CLogger logger = Utils.getLogger(TestPolicyForSnapshot.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/volumeSnapshot/TestPolicyForVolumeSnapshot.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        BackupStorageInventory bs = deployer.backupStorages.get("sftp");

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        identityCreator.createUser("user1", "password");
        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APICreateVolumeSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APIRevertVolumeFromSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APIDeleteVolumeSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APIBackupVolumeSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APIDeleteVolumeSnapshotFromBackupStorageMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APIUpdateVolumeSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APICreateDataVolumeFromVolumeSnapshotMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user1", "allow");
        SessionInventory session = identityCreator.userLogin("user1", "password");

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VolumeInventory root = vm.getRootVolume();
        api.stopVmInstance(vm.getUuid());

        VolumeSnapshotInventory sp = api.createSnapshot(root.getUuid(), session);
        api.updateVolumeSnapshot(sp, session);
        api.revertVolumeToSnapshot(sp.getUuid(), session);
        api.createTemplateFromSnapshot(sp.getUuid(), bs.getUuid(), session);
        api.createDataVolumeFromSnapshot(sp.getUuid(), session);
        sp = api.backupSnapshot(sp.getUuid(), null, session);
        api.deleteSnapshotFromBackupStorage(sp.getUuid(), session, sp.getBackupStorageRefs().get(0).getBackupStorageUuid());
        api.deleteSnapshot(sp.getUuid(), session);

        sp = api.createSnapshot(root.getUuid(), session);
        identityCreator.detachPolicyFromUser("user1", "allow");

        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APICreateVolumeSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APIRevertVolumeFromSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APIDeleteVolumeSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APIBackupVolumeSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APIDeleteVolumeSnapshotFromBackupStorageMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeSnapshotConstant.ACTION_CATEGORY, APIUpdateVolumeSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VolumeConstant.ACTION_CATEGORY, APICreateDataVolumeFromVolumeSnapshotMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean success = false;
        try {
            api.updateVolumeSnapshot(sp, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.revertVolumeToSnapshot(sp.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createTemplateFromSnapshot(sp.getUuid(), bs.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createDataVolumeFromSnapshot(sp.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            sp = api.backupSnapshot(sp.getUuid(), null, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteSnapshotFromBackupStorage(sp.getUuid(), session, bs.getUuid());
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteSnapshot(sp.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createSnapshot(root.getUuid(), session);
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

        sp = api.createSnapshot(root.getUuid(), session);
        api.updateVolumeSnapshot(sp, session);
        api.revertVolumeToSnapshot(sp.getUuid(), session);
        api.createTemplateFromSnapshot(sp.getUuid(), bs.getUuid(), session);
        api.createDataVolumeFromSnapshot(sp.getUuid(), session);
        sp = api.backupSnapshot(sp.getUuid(), null, session);
        api.deleteSnapshotFromBackupStorage(sp.getUuid(), session, sp.getBackupStorageRefs().get(0).getBackupStorageUuid());
        api.deleteSnapshot(sp.getUuid(), session);

        sp = api.createSnapshot(root.getUuid(), session);
        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");

        success = false;
        try {
            api.updateVolumeSnapshot(sp, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.revertVolumeToSnapshot(sp.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createTemplateFromSnapshot(sp.getUuid(), bs.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createDataVolumeFromSnapshot(sp.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            sp = api.backupSnapshot(sp.getUuid(), null, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteSnapshotFromBackupStorage(sp.getUuid(), session, bs.getUuid());
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteSnapshot(sp.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createSnapshot(root.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryVolumeSnapshotMsg qmsg = new APIQueryVolumeSnapshotMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qmsg, APIQueryVolumeSnapshotReply.class, session);

        APIQueryVolumeSnapshotTreeMsg tmsg = new APIQueryVolumeSnapshotTreeMsg();
        tmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(tmsg, APIQueryVolumeSnapshotTreeReply.class, session);

        VolumeSnapshotInventory spd = api.createSnapshot(root.getUuid());
        api.changeResourceOwner(spd.getUuid(), identityCreator.getAccountSession().getAccountUuid());
        api.deleteSnapshot(spd.getUuid(), identityCreator.getAccountSession());
    }
}
