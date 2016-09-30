package org.zstack.test.tag;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.tag.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.ArrayList;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 */
public class TestPolicyForTag {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/tag/TestPolicyForTag.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        identityCreator.createUser("user1", "password");

        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", TagConstant.ACTION_CATEGORY, APICreateUserTagMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", TagConstant.ACTION_CATEGORY, APICreateSystemTagMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", TagConstant.ACTION_CATEGORY, APIDeleteTagMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user1", "allow");
        SessionInventory session = identityCreator.userLogin("user1", "password");

        TagInventory utag = api.createUserTag(vm.getUuid(), "test", VmInstanceVO.class, session);
        TagInventory stag = api.createSystemTag(vm.getUuid(), VmSystemTags.HOSTNAME.instantiateTag(map(e("hostname", "vm.zstack.org"))), VmInstanceVO.class, session);
        api.deleteTag(utag.getUuid(), session);
        api.deleteTag(stag.getUuid(), session);

        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", TagConstant.ACTION_CATEGORY, APICreateUserTagMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", TagConstant.ACTION_CATEGORY, APICreateSystemTagMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", TagConstant.ACTION_CATEGORY, APIDeleteTagMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);

        utag = api.createUserTag(vm.getUuid(), "test", VmInstanceVO.class, session);
        stag = api.createSystemTag(vm.getUuid(), VmSystemTags.HOSTNAME.instantiateTag(map(e("hostname", "vm.zstack.org"))), VmInstanceVO.class, session);

        identityCreator.detachPolicyFromUser("user1", "allow");
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean success = false;
        try {
            api.createUserTag(vm.getUuid(), "test", VmInstanceVO.class, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createSystemTag(vm.getUuid(), VmSystemTags.HOSTNAME.instantiateTag(map(e("hostname", "vm.zstack.org"))), VmInstanceVO.class, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteTag(utag.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteTag(stag.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.deleteTag(utag.getUuid());
        api.deleteTag(stag.getUuid());

        // user and group
        identityCreator.createUser("user2", "password");
        identityCreator.createGroup("group");
        identityCreator.addUserToGroup("user2", "group");
        identityCreator.attachPolicyToGroup("group", "allow");
        session = identityCreator.userLogin("user2", "password");

        utag = api.createUserTag(vm.getUuid(), "test", VmInstanceVO.class, session);
        stag = api.createSystemTag(vm.getUuid(), VmSystemTags.HOSTNAME.instantiateTag(map(e("hostname", "vm.zstack.org"))), VmInstanceVO.class, session);
        api.deleteTag(utag.getUuid(), session);
        api.deleteTag(stag.getUuid(), session);

        utag = api.createUserTag(vm.getUuid(), "test", VmInstanceVO.class, session);
        stag = api.createSystemTag(vm.getUuid(), VmSystemTags.HOSTNAME.instantiateTag(map(e("hostname", "vm.zstack.org"))), VmInstanceVO.class, session);

        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");

        success = false;
        try {
            api.createUserTag(vm.getUuid(), "test", VmInstanceVO.class, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.createSystemTag(vm.getUuid(), VmSystemTags.HOSTNAME.instantiateTag(map(e("hostname", "vm.zstack.org"))), VmInstanceVO.class, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteTag(utag.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteTag(stag.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryUserTagMsg umsg = new APIQueryUserTagMsg();
        umsg.setConditions(new ArrayList<QueryCondition>());
        api.query(umsg, APIQueryUserTagReply.class, session);

        APIQuerySystemTagMsg smsg = new APIQuerySystemTagMsg();
        smsg.setConditions(new ArrayList<QueryCondition>());
        api.query(smsg, APIQuerySystemTagReply.class, session);

        IdentityCreator identityCreator1 = new IdentityCreator(api);
        identityCreator1.createAccount("test2", "password");
        session = identityCreator1.accountLogin("test2", "password");

        success = false;
        try {
            api.deleteTag(utag.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteTag(stag.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.deleteTag(utag.getUuid());
        api.deleteTag(stag.getUuid());
    }
}
