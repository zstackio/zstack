package org.zstack.test.virtualrouter;

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
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.portforwarding.*;
import org.zstack.network.service.vip.APICreateVipMsg;
import org.zstack.network.service.vip.VipConstant;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.ArrayList;

/**
 *
 */
public class TestPolicyForPortForwarding {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestPolicyForPortForwarding.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    PortForwardingRuleInventory createPortForwarding(String l3Uuid, SessionInventory session) throws ApiSenderException {
        PortForwardingRuleInventory rule1 = new PortForwardingRuleInventory();
        VipInventory vip = api.acquireIp(l3Uuid, session);
        rule1.setName("pfRule1");
        rule1.setVipUuid(vip.getUuid());
        rule1.setVipPortStart(22);
        rule1.setVipPortEnd(100);
        rule1.setPrivatePortStart(22);
        rule1.setPrivatePortEnd(100);
        rule1.setProtocolType(PortForwardingProtocolType.TCP.toString());
        return api.createPortForwardingRuleByFullConfig(rule1, session);
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory vipNw = deployer.l3Networks.get("PublicNetwork");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        identityCreator.createUser("user1", "password");
        Statement s = new Statement();
        s.setEffect(StatementEffect.Allow);
        s.setName("allowvip");
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APICreateVipMsg.class.getSimpleName()));
        identityCreator.createPolicy("allowvip", s);

        s = new Statement();
        s.setEffect(StatementEffect.Allow);
        s.setName("allow");
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APICreatePortForwardingRuleMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APIUpdatePortForwardingRuleMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APIChangePortForwardingRuleStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APIAttachPortForwardingRuleMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APIDetachPortForwardingRuleMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APIDeletePortForwardingRuleMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        SessionInventory session = identityCreator.userLogin("user1", "password");

        identityCreator.attachPolicyToUser("user1", "allowvip");
        identityCreator.attachPolicyToUser("user1", "allow");


        PortForwardingRuleInventory rule = createPortForwarding(vipNw.getUuid(), session);
        api.attachPortForwardingRule(rule.getUuid(), nic.getUuid(), session);
        api.detachPortForwardingRule(rule.getUuid(), session);
        api.updatePortForwardingRule(rule, session);
        api.changePortForwardingRuleState(rule.getUuid(), PortForwardingRuleStateEvent.disable, session);
        api.revokePortForwardingRule(rule.getUuid(), session);

        rule = createPortForwarding(vipNw.getUuid(), session);

        s = new Statement();
        s.setEffect(StatementEffect.Deny);
        s.setName("deny");
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APICreatePortForwardingRuleMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APIUpdatePortForwardingRuleMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APIChangePortForwardingRuleStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APIAttachPortForwardingRuleMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APIDetachPortForwardingRuleMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", PortForwardingConstant.ACTION_CATEGORY, APIDeletePortForwardingRuleMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);

        identityCreator.detachPolicyFromUser("user1", "allow");
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean success = false;
        try {
            api.attachPortForwardingRule(rule.getUuid(), nic.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.detachPortForwardingRule(rule.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updatePortForwardingRule(rule, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changePortForwardingRuleState(rule.getUuid(), PortForwardingRuleStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.revokePortForwardingRule(rule.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            createPortForwarding(vipNw.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.revokePortForwardingRule(rule.getUuid());

        // user and group

        identityCreator.createUser("user2", "password");
        identityCreator.createGroup("group");
        identityCreator.addUserToGroup("user2", "group");
        identityCreator.attachPolicyToGroup("group", "allowvip");
        identityCreator.attachPolicyToGroup("group", "allow");
        session = identityCreator.userLogin("user2", "password");

        rule = createPortForwarding(vipNw.getUuid(), session);
        api.attachPortForwardingRule(rule.getUuid(), nic.getUuid(), session);
        api.detachPortForwardingRule(rule.getUuid(), session);
        api.updatePortForwardingRule(rule, session);
        api.changePortForwardingRuleState(rule.getUuid(), PortForwardingRuleStateEvent.disable, session);
        api.revokePortForwardingRule(rule.getUuid(), session);

        rule = createPortForwarding(vipNw.getUuid(), session);
        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");

        success = false;
        try {
            api.attachPortForwardingRule(rule.getUuid(), nic.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.detachPortForwardingRule(rule.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updatePortForwardingRule(rule, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changePortForwardingRuleState(rule.getUuid(), PortForwardingRuleStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.revokePortForwardingRule(rule.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            createPortForwarding(vipNw.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryPortForwardingRuleMsg qmsg = new APIQueryPortForwardingRuleMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qmsg, APIQueryPortForwardingRuleReply.class, session);
    }
}
