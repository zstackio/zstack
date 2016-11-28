package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.UserInventory;
import org.zstack.header.vm.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * test checking user policies
 */
public class TestPolicyForVm6 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestPolicyForVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        UserInventory user = identityCreator.createUser("user", "password");

        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APICreateVmInstanceMsg.class.getName()));
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIDestroyVmInstanceMsg.class.getName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user", "allow");

        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIRebootVmInstanceMsg.class.getName()));
        identityCreator.createPolicy("deny", s);
        identityCreator.attachPolicyToUser("user", "deny");

        List<String> apiNames = list(APICreateVmInstanceMsg.class.getName(), APIDestroyVmInstanceMsg.class.getName(),
                APIRebootVmInstanceMsg.class.getName(), APIStartVmInstanceMsg.class.getName());

        Map<String, String> ret = api.checkUserPolicy(apiNames, user.getUuid(), null);
        Assert.assertEquals(StatementEffect.Allow.toString(), ret.get(APICreateVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Allow.toString(), ret.get(APIDestroyVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Deny.toString(), ret.get(APIRebootVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Deny.toString(), ret.get(APIStartVmInstanceMsg.class.getName()));

        SessionInventory session = identityCreator.userLogin(user.getName(), "password");
        ret = api.checkUserPolicy(apiNames, null, session);
        Assert.assertEquals(StatementEffect.Allow.toString(), ret.get(APICreateVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Allow.toString(), ret.get(APIDestroyVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Deny.toString(), ret.get(APIRebootVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Deny.toString(), ret.get(APIStartVmInstanceMsg.class.getName()));

        identityCreator.createGroup("group");
        identityCreator.addUserToGroup("user", "group");
        s = new Statement();
        s.setName("group-allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIStartVmInstanceMsg.class.getName()));
        identityCreator.createPolicy("group-allow", s);
        identityCreator.attachPolicyToGroup("group", "group-allow");
        ret = api.checkUserPolicy(apiNames, null, session);
        Assert.assertEquals(StatementEffect.Allow.toString(), ret.get(APICreateVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Allow.toString(), ret.get(APIDestroyVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Deny.toString(), ret.get(APIRebootVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Allow.toString(), ret.get(APIStartVmInstanceMsg.class.getName()));

        // user can test own permissions
        ret = api.checkUserPolicy(apiNames, user.getUuid(), session);
        Assert.assertEquals(StatementEffect.Allow.toString(), ret.get(APICreateVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Allow.toString(), ret.get(APIDestroyVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Deny.toString(), ret.get(APIRebootVmInstanceMsg.class.getName()));
        Assert.assertEquals(StatementEffect.Allow.toString(), ret.get(APIStartVmInstanceMsg.class.getName()));
    }
}
