package org.zstack.test.vip;

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
import org.zstack.network.service.vip.*;
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
 * 2. assign permissions of allow of creating/updating/changing/deleting VIP to the user
 * <p>
 * confirm the user can do those operations
 * <p>
 * 3. assign permissions of deny of creating/updating/changing/deleting VIP to the user
 * <p>
 * confirm the user cannot do those operations
 * <p>
 * 4. create a user added in a group
 * 5. assign permissions of allow of creating/updating/changing/deleting VIP to the group
 * <p>
 * confirm the user can do those operations
 * <p>
 * 6. assign permissions of deny of creating/updating/changing/deleting VIP to the group
 * <p>
 * confirm the user cannot do those operations
 */
public class TestPolicyForVip {
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
        deployer = new Deployer("deployerXml/vip/TestPolicyForVip.xml", con);
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

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory pubL3 = deployer.l3Networks.get("PublicNetwork");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        identityCreator.createUser("user1", "password");
        Statement s = new Statement();
        s.setEffect(StatementEffect.Allow);
        s.setName("allow");
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APICreateVipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APIUpdateVipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APIChangeVipStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APIDeleteVipMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user1", "allow");
        SessionInventory session = identityCreator.userLogin("user1", "password");

        VipInventory vip = api.acquireIp(pubL3.getUuid(), session);
        api.updateVip(vip, session);
        api.changeVipSate(vip.getUuid(), VipStateEvent.disable, session);
        api.releaseIp(vip.getUuid(), session);

        vip = api.acquireIp(pubL3.getUuid(), session);
        identityCreator.detachPolicyFromUser("user1", "allow");
        s = new Statement();
        s.setEffect(StatementEffect.Deny);
        s.setName("deny");
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APICreateVipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APIUpdateVipMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APIChangeVipStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APIDeleteVipMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean success = false;
        try {
            api.updateVip(vip, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changeVipSate(vip.getUuid(), VipStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.releaseIp(vip.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.acquireIp(pubL3.getUuid(), session);
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

        vip = api.acquireIp(pubL3.getUuid(), session);
        api.updateVip(vip, session);
        api.changeVipSate(vip.getUuid(), VipStateEvent.disable, session);
        api.releaseIp(vip.getUuid(), session);

        vip = api.acquireIp(pubL3.getUuid(), session);
        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");

        success = false;
        try {
            api.updateVip(vip, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changeVipSate(vip.getUuid(), VipStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.releaseIp(vip.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.acquireIp(pubL3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryVipMsg qmsg = new APIQueryVipMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qmsg, APIQueryVipReply.class, session);
    }
}
