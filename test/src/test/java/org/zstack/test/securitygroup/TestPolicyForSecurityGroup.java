package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.securitygroup.*;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;

import static org.zstack.utils.CollectionDSL.list;

public class TestPolicyForSecurityGroup {
    static CLogger logger = Utils.getLogger(TestPolicyForSecurityGroup.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestPolicyForSecurityGroup.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        identityCreator.createUser("user1", "password");
        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APICreateSecurityGroupMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIUpdateSecurityGroupMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIChangeSecurityGroupStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIAttachSecurityGroupToL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIDetachSecurityGroupFromL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIAddSecurityGroupRuleMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIAddVmNicToSecurityGroupMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIDeleteVmNicFromSecurityGroupMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIDeleteSecurityGroupMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIDeleteSecurityGroupRuleMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user1", "allow");
        SessionInventory session = identityCreator.userLogin("user1", "password");

        SecurityGroupInventory sg = api.createSecurityGroup("test", session);
        SecurityGroupRuleAO r = new SecurityGroupRuleAO();
        r.setStartPort(10);
        r.setStartPort(100);
        r.setProtocol(SecurityGroupRuleProtocolType.TCP.toString());
        r.setType(SecurityGroupRuleType.Ingress.toString());
        sg = api.addSecurityGroupRuleByFullConfig(sg.getUuid(), list(r), session);
        SecurityGroupRuleInventory rule = sg.getRules().get(0);
        api.attachSecurityGroupToL3Network(sg.getUuid(), l3.getUuid(), session);
        api.addVmNicToSecurityGroup(sg.getUuid(), list(nic.getUuid()), session);
        api.removeVmNicFromSecurityGroup(sg.getUuid(), nic.getUuid(), session);
        api.detachSecurityGroupFromL3Network(sg.getUuid(), l3.getUuid(), session);
        api.removeSecurityGroupRule(list(rule.getUuid()), session);
        api.updateSecurityGroup(sg, session);
        api.changeSecurityGroupState(sg.getUuid(), SecurityGroupStateEvent.disable, session);
        api.deleteSecurityGroup(sg.getUuid(), session);

        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APICreateSecurityGroupMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIUpdateSecurityGroupMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIChangeSecurityGroupStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIAttachSecurityGroupToL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIDetachSecurityGroupFromL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIAddVmNicToSecurityGroupMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIAddSecurityGroupRuleMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIDeleteVmNicFromSecurityGroupMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIDeleteSecurityGroupMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", SecurityGroupConstant.ACTION_CATEGORY, APIDeleteSecurityGroupRuleMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);

        sg = api.createSecurityGroup("test", session);
        r = new SecurityGroupRuleAO();
        r.setStartPort(10);
        r.setStartPort(100);
        r.setProtocol(SecurityGroupRuleProtocolType.TCP.toString());
        r.setType(SecurityGroupRuleType.Ingress.toString());
        sg = api.addSecurityGroupRuleByFullConfig(sg.getUuid(), list(r), session);
        rule = sg.getRules().get(0);

        identityCreator.detachPolicyFromUser("user1", "allow");
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean success = false;
        try {
            api.createSecurityGroup("test", session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.addSecurityGroupRuleByFullConfig(sg.getUuid(), list(r), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.attachSecurityGroupToL3Network(sg.getUuid(), l3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.addVmNicToSecurityGroup(sg.getUuid(), list(nic.getUuid()), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.removeVmNicFromSecurityGroup(sg.getUuid(), nic.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.detachSecurityGroupFromL3Network(sg.getUuid(), l3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.removeSecurityGroupRule(list(rule.getUuid()), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updateSecurityGroup(sg, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changeSecurityGroupState(sg.getUuid(), SecurityGroupStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteSecurityGroup(sg.getUuid(), session);
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

        sg = api.createSecurityGroup("test", session);
        r = new SecurityGroupRuleAO();
        r.setStartPort(10);
        r.setStartPort(100);
        r.setProtocol(SecurityGroupRuleProtocolType.TCP.toString());
        r.setType(SecurityGroupRuleType.Ingress.toString());
        sg = api.addSecurityGroupRuleByFullConfig(sg.getUuid(), list(r), session);
        rule = sg.getRules().get(0);
        api.attachSecurityGroupToL3Network(sg.getUuid(), l3.getUuid(), session);
        api.addVmNicToSecurityGroup(sg.getUuid(), list(nic.getUuid()), session);
        api.removeVmNicFromSecurityGroup(sg.getUuid(), nic.getUuid(), session);
        api.detachSecurityGroupFromL3Network(sg.getUuid(), l3.getUuid(), session);
        api.removeSecurityGroupRule(list(rule.getUuid()), session);
        api.updateSecurityGroup(sg, session);
        api.changeSecurityGroupState(sg.getUuid(), SecurityGroupStateEvent.disable, session);
        api.deleteSecurityGroup(sg.getUuid(), session);


        sg = api.createSecurityGroup("test", session);
        r = new SecurityGroupRuleAO();
        r.setStartPort(10);
        r.setStartPort(100);
        r.setProtocol(SecurityGroupRuleProtocolType.TCP.toString());
        r.setType(SecurityGroupRuleType.Ingress.toString());
        sg = api.addSecurityGroupRuleByFullConfig(sg.getUuid(), list(r), session);
        rule = sg.getRules().get(0);

        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");

        success = false;
        try {
            api.createSecurityGroup("test", session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.addSecurityGroupRuleByFullConfig(sg.getUuid(), list(r), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.attachSecurityGroupToL3Network(sg.getUuid(), l3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.addVmNicToSecurityGroup(sg.getUuid(), list(nic.getUuid()), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.removeVmNicFromSecurityGroup(sg.getUuid(), nic.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.detachSecurityGroupFromL3Network(sg.getUuid(), l3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.removeSecurityGroupRule(list(rule.getUuid()), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updateSecurityGroup(sg, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changeSecurityGroupState(sg.getUuid(), SecurityGroupStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteSecurityGroup(sg.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQuerySecurityGroupMsg qmsg = new APIQuerySecurityGroupMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qmsg, APIQuerySecurityGroupReply.class, session);

        APIQuerySecurityGroupRuleMsg rmsg = new APIQuerySecurityGroupRuleMsg();
        rmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(rmsg, APIQuerySecurityGroupRuleReply.class, session);
    }
}
