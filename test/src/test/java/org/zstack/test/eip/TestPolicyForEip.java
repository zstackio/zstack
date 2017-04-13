package org.zstack.test.eip;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import org.zstack.network.service.eip.*;
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
 * 1. create a user
 * 2. assign permissions of allow of creating/changing/updating/attaching/detaching/deleting eip to the user
 * <p>
 * confirm the user can do those operations
 * <p>
 * 3. assign permissions of deny of creating/changing/updating/attaching/detaching/deleting eip to the user
 * <p>
 * confirm the user cannot do those operations
 * <p>
 * 4. create a user added in a group
 * 5. assign permissions of allow of creating/changing/updating/attaching/detaching/deleting eip to the group
 * <p>
 * confirm the user can do those operations
 * <p>
 * 6. assign permissions of deny of creating/changing/updating/attaching/detaching/deleting eip to the group
 * <p>
 * confirm the user cannot do those operations
 */
@Ignore
@Deprecated
// New Groovy Case : TestPolicyForEip.groovy
public class TestPolicyForEip {
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
        deployer = new Deployer("deployerXml/eip/TestPolicyForEip.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    private EipInventory createEip(String l3Uuid, String nicUuid, SessionInventory session) throws ApiSenderException {
        VipInventory vip = api.acquireIp(l3Uuid, session);
        return api.createEip("eip", vip.getUuid(), nicUuid, session);
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory l3 = deployer.l3Networks.get("PublicNetwork");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        identityCreator.createUser("user1", "password");
        Statement s = new Statement();
        s.setName("allowvip");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APICreateVipMsg.class.getSimpleName()));
        identityCreator.createPolicy("allowvip", s);

        s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APICreateEipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIUpdateEipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIChangeEipStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIAttachEipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIDetachEipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIDeleteEipMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user1", "allowvip");
        identityCreator.attachPolicyToUser("user1", "allow");
        SessionInventory session = identityCreator.userLogin("user1", "password");

        EipInventory eip = createEip(l3.getUuid(), nic.getUuid(), session);
        api.updateEip(eip, session);
        api.detachEip(eip.getUuid(), session);
        api.attachEip(eip.getUuid(), nic.getUuid(), session);
        api.changeEipState(eip.getUuid(), EipStateEvent.disable, session);
        api.removeEip(eip.getUuid(), session);

        eip = createEip(l3.getUuid(), nic.getUuid(), session);
        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APICreateEipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIUpdateEipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIChangeEipStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIAttachEipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIDetachEipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIDeleteEipMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);
        identityCreator.detachPolicyFromUser("user1", "allow");
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean success = false;
        try {
            api.updateEip(eip, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.detachEip(eip.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.attachEip(eip.getUuid(), nic.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changeEipState(eip.getUuid(), EipStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.removeEip(eip.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            createEip(l3.getUuid(), nic.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);
        api.removeEip(eip.getUuid());

        // user and group
        identityCreator.createUser("user2", "password");
        identityCreator.createGroup("group");
        identityCreator.addUserToGroup("user2", "group");
        identityCreator.attachPolicyToGroup("group", "allowvip");
        identityCreator.attachPolicyToGroup("group", "allow");
        session = identityCreator.userLogin("user2", "password");

        eip = createEip(l3.getUuid(), nic.getUuid(), session);
        api.updateEip(eip, session);
        api.detachEip(eip.getUuid(), session);
        api.attachEip(eip.getUuid(), nic.getUuid(), session);
        api.changeEipState(eip.getUuid(), EipStateEvent.disable, session);
        api.removeEip(eip.getUuid(), session);

        eip = createEip(l3.getUuid(), nic.getUuid(), session);
        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");

        success = false;
        try {
            api.updateEip(eip, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.detachEip(eip.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.attachEip(eip.getUuid(), nic.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changeEipState(eip.getUuid(), EipStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.removeEip(eip.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            createEip(l3.getUuid(), nic.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryEipMsg qmsg = new APIQueryEipMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qmsg, APIQueryEipReply.class, session);
    }
}
