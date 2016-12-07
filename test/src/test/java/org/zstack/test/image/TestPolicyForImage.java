package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageDetails;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.test.*;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * 1. create a user
 * 2. assign permissions of allow of creating/changing/updating/deleting to the user
 * <p>
 * confirm the user can create/change/update/delete the image
 * <p>
 * 3. assign permissions of deny of creating/changing/updating/deleting to the user
 * <p>
 * confirm the user cannot create/change/update/delete the image
 * <p>
 * 4. create a user added to a group
 * 5. assign permissions of allow of creating/changing/updating/deleting to the group
 * <p>
 * confirm the user can create/change/update/delete the image
 * <p>
 * 6. assign permissions of deny of creating/changing/updating/deleting to the group
 * <p>
 * confirm the user cannot create/change/update/delete the image
 */
public class TestPolicyForImage {
    CLogger logger = Utils.getLogger(TestPolicyForImage.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("Simulator.xml").addXml("BackupStorageManager.xml")
                .addXml("ImageManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    private ImageInventory createImage(String bsUuid, SessionInventory session) throws ApiSenderException {
        ImageInventory iinv = new ImageInventory();
        iinv.setName("Test Image");
        iinv.setDescription("Test Image");
        iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        iinv.setGuestOsType("Window7");
        iinv.setFormat("simulator");
        iinv.setUrl("http://zstack.org/download/win7.qcow2");
        return api.addImage(iinv, session, bsUuid);
    }

    @Test
    public void test() throws ApiSenderException {
        SimulatorBackupStorageDetails ss = new SimulatorBackupStorageDetails();
        ss.setTotalCapacity(SizeUnit.GIGABYTE.toByte(100));
        ss.setUsedCapacity(0);
        ss.setUrl("nfs://simulator/backupstorage/");
        BackupStorageInventory inv = api.createSimulatorBackupStorage(1, ss).get(0);
        BackupStorageVO vo = dbf.findByUuid(inv.getUuid(), BackupStorageVO.class);
        Assert.assertNotNull(vo);

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.createAccount("test", "password");
        identityCreator.createUser("user1", "password");
        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APIAddImageMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APIUpdateImageMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APIChangeImageStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APIDeleteImageMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user1", "allow");
        SessionInventory session = identityCreator.userLogin("user1", "password");

        ImageInventory img = createImage(vo.getUuid(), session);
        api.changeImageState(img.getUuid(), ImageStateEvent.disable, session);
        api.deleteImage(img.getUuid(), session);

        img = createImage(vo.getUuid(), session);
        identityCreator.detachPolicyFromUser("user1", "allow");
        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APIAddImageMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APIUpdateImageMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APIChangeImageStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ImageConstant.ACTION_CATEGORY, APIDeleteImageMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);
        identityCreator.attachPolicyToUser("user1", "deny");
        identityCreator.detachPolicyFromUser("user1", "allow");

        boolean success = false;
        try {
            api.changeImageState(img.getUuid(), ImageStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteImage(img.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            createImage(vo.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        // user and group
        identityCreator.createGroup("group");
        identityCreator.createUser("user2", "password");
        identityCreator.addUserToGroup("user2", "group");
        identityCreator.attachPolicyToGroup("group", "allow");
        session = identityCreator.userLogin("user2", "password");

        img = createImage(vo.getUuid(), session);
        api.changeImageState(img.getUuid(), ImageStateEvent.disable);
        api.deleteImage(img.getUuid());

        img = createImage(vo.getUuid(), session);

        identityCreator.attachPolicyToGroup("group", "deny");
        identityCreator.detachPolicyFromGroup("group", "allow");

        success = false;
        try {
            api.changeImageState(img.getUuid(), ImageStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteImage(img.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            createImage(vo.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        // make all image shared to public
        SimpleQuery<ImageVO> imgq = dbf.createQuery(ImageVO.class);
        imgq.select(ImageVO_.uuid);
        List<String> uuids = imgq.listValue();

        api.shareResource(uuids, null, true);

        APIQueryImageMsg qmsg = new APIQueryImageMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        APIQueryImageReply r = api.query(qmsg, APIQueryImageReply.class, session);
        ImageInventory imginv = r.getInventories().get(0);
        imginv.setName("xxx");
        imginv.setFormat(null);
        api.updateImage(imginv);

        // test condition query works with normal account query,
        // there was a bug caused by AccountSubQueryExtension
        qmsg = new APIQueryImageMsg();
        qmsg.addQueryCondition("name", QueryOp.LIKE, "%xx%");
        r = api.query(qmsg, APIQueryImageReply.class, session);
        Assert.assertEquals(1, r.getInventories().size());
        ImageInventory imginv1 = r.getInventories().get(0);
        Assert.assertEquals(imginv.getUuid(), imginv1.getUuid());
    }
}
