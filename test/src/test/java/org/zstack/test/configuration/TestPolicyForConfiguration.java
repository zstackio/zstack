package org.zstack.test.configuration;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.*;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.test.*;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.test.image.TestAddImage;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;

/**
 * 1. create a user
 * 2. assign permissions of allow of creating/deleting/changing/updating instance offering and disk offering to the user
 * <p>
 * confirm the user can create/delete/change/update the instance offering and disk offering
 * <p>
 * 3. assign permissions of deny of creating/deleting/changing/updating instance offering and disk offering to the user
 * <p>
 * confirm the user can not create/delete/change/update the instance offering and disk offering
 * <p>
 * 4. create a group
 * 5. create another user
 * 6. add the user to the group
 * 7. assign permissions of allow of creating/deleting/changing/updating instance offering and disk offering to the group
 * <p>
 * confirm the group can create/delete/change/update the instance offering and disk offering
 * <p>
 * 7. assign permissions of deny of creating/deleting/changing/updating instance offering and disk offering to the group
 * <p>
 * confirm the group cannot create/delete/change/update the instance offering and disk offering
 * <p>
 * confirm the user can query the instance offering and disk offering
 */
public class TestPolicyForConfiguration {
    CLogger logger = Utils.getLogger(TestAddImage.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("ConfigurationManager.xml").addXml("HostAllocatorManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    private InstanceOfferingInventory createInstanceOffering(SessionInventory session) throws ApiSenderException {
        InstanceOfferingInventory inv = new InstanceOfferingInventory();
        inv.setName("TestInstanceOffering");
        inv.setCpuNum(2);
        inv.setCpuSpeed(1000);
        inv.setMemorySize(SizeUnit.GIGABYTE.toByte(1));
        inv.setDescription("TestInstanceOffering");
        return api.addInstanceOffering(inv, session);
    }

    private DiskOfferingInventory createDiskOffering(SessionInventory session) throws ApiSenderException {
        DiskOfferingInventory d = new DiskOfferingInventory();
        d.setName("test");
        d.setDiskSize(1000);
        return api.addDiskOfferingByFullConfig(d, session);
    }

    @Test
    public void test() throws ApiSenderException {
        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.createAccount("test", "password");
        identityCreator.createUser("user1", "password");
        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APICreateInstanceOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIChangeInstanceOfferingStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIUpdateInstanceOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIDeleteInstanceOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APICreateDiskOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIChangeDiskOfferingStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIUpdateDiskOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIDeleteDiskOfferingMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user1", "allow");

        SessionInventory session = identityCreator.userLogin("user1", "password");

        InstanceOfferingInventory ioinv = createInstanceOffering(session);
        api.changeInstanceOfferingState(ioinv.getUuid(), InstanceOfferingStateEvent.disable, session);
        api.updateInstanceOffering(ioinv, session);
        api.deleteInstanceOffering(ioinv.getUuid(), session);
        DiskOfferingInventory doinv = createDiskOffering(session);
        api.updateDiskOffering(doinv, session);
        api.changeDiskOfferingState(doinv.getUuid(), DiskOfferingStateEvent.disable, session);
        api.deleteDiskOffering(doinv.getUuid(), session);


        ioinv = createInstanceOffering(session);
        doinv = createDiskOffering(session);

        identityCreator.detachPolicyFromUser("user1", "allow");

        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APICreateInstanceOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIChangeInstanceOfferingStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIUpdateInstanceOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIDeleteInstanceOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APICreateDiskOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIChangeDiskOfferingStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIUpdateDiskOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIDeleteDiskOfferingMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean success = false;
        try {
            api.changeInstanceOfferingState(ioinv.getUuid(), InstanceOfferingStateEvent.disable, session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updateInstanceOffering(ioinv, session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteInstanceOffering(ioinv.getUuid(), session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            createInstanceOffering(session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updateDiskOffering(doinv, session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changeDiskOfferingState(doinv.getUuid(), DiskOfferingStateEvent.disable, session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteDiskOffering(doinv.getUuid(), session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            createDiskOffering(session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        // user2 and group
        identityCreator.createGroup("group");
        identityCreator.createUser("user2", "password");
        identityCreator.addUserToGroup("user2", "group");
        identityCreator.attachPolicyToGroup("group", "allow");
        session = identityCreator.userLogin("user2", "password");

        ioinv = createInstanceOffering(session);
        api.changeInstanceOfferingState(ioinv.getUuid(), InstanceOfferingStateEvent.disable, session);
        api.updateInstanceOffering(ioinv, session);
        api.deleteInstanceOffering(ioinv.getUuid(), session);
        doinv = createDiskOffering(session);
        api.updateDiskOffering(doinv, session);
        api.changeDiskOfferingState(doinv.getUuid(), DiskOfferingStateEvent.disable, session);
        api.deleteDiskOffering(doinv.getUuid(), session);

        ioinv = createInstanceOffering(session);
        doinv = createDiskOffering(session);

        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");

        success = false;
        try {
            api.changeInstanceOfferingState(ioinv.getUuid(), InstanceOfferingStateEvent.disable, session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updateInstanceOffering(ioinv, session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteInstanceOffering(ioinv.getUuid(), session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            createInstanceOffering(session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updateDiskOffering(doinv, session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changeDiskOfferingState(doinv.getUuid(), DiskOfferingStateEvent.disable, session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteDiskOffering(doinv.getUuid(), session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            createDiskOffering(session);
        } catch (Exception e) {
            if (e instanceof ApiSenderException && IdentityErrors.PERMISSION_DENIED.toString().equals(((ApiSenderException) e).getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryInstanceOfferingMsg qimsg = new APIQueryInstanceOfferingMsg();
        qimsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qimsg, APIQueryInstanceOfferingReply.class, session);

        APIQueryDiskOfferingMsg qdmsg = new APIQueryDiskOfferingMsg();
        qdmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qdmsg, APIQueryDiskOfferingMsg.class, session);
    }
}
