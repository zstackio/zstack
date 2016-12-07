package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.*;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.query.QueryOp;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;

/**
 * 1. create a user
 * 2. create a policy
 * 3. attach the policy to the user
 * 4. delete the user
 * <p>
 * confirm the policy is detached
 * <p>
 * 5. create another user
 * 6. attach the policy to the user
 * 7. delete the policy
 * <p>
 * confirm the policy is detached
 * <p>
 * 8. create a group and a policy
 * 9. attach the policy to the group
 * 10. delete the group
 * <p>
 * confirm the policy is detached
 * <p>
 * 11. create another group and attach the policy
 * 12. delete the policy
 * <p>
 * confirm the policy is detached
 */
public class TestIdentity7 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        IdentityCreator creator = new IdentityCreator(api);
        creator.createAccount("test", "test");
        UserInventory u = creator.createUser("test", "test");

        Statement s = new Statement();
        s.addAction(".*");
        s.setEffect(StatementEffect.Allow);
        PolicyInventory p = creator.createPolicy("test", s);
        creator.attachPolicyToUser("test", "test");

        SimpleQuery<UserPolicyRefVO> q = dbf.createQuery(UserPolicyRefVO.class);
        q.add(UserPolicyRefVO_.userUuid, Op.EQ, u.getUuid());
        q.add(UserPolicyRefVO_.policyUuid, Op.EQ, p.getUuid());
        Assert.assertTrue(q.isExists());

        creator.deleteUser("test");
        Assert.assertFalse(q.isExists());

        creator.createUser("user1", "password");
        creator.attachPolicyToUser("user1", "test");
        creator.deletePolicy("test");
        Assert.assertFalse(q.isExists());

        UserGroupInventory g = creator.createGroup("group");
        p = creator.createPolicy("policy", s);
        creator.attachPolicyToGroup("group", "policy");

        APIQueryPolicyMsg msg = new APIQueryPolicyMsg();
        msg.addQueryCondition("group.uuid", QueryOp.EQ, g.getUuid());
        APIQueryPolicyReply reply = api.query(msg, APIQueryPolicyReply.class, creator.getAccountSession());
        Assert.assertEquals(1, reply.getInventories().size());
        PolicyInventory retp = reply.getInventories().get(0);
        Assert.assertEquals("policy", retp.getName());

        SimpleQuery<UserGroupPolicyRefVO> pq = dbf.createQuery(UserGroupPolicyRefVO.class);
        pq.add(UserGroupPolicyRefVO_.policyUuid, Op.EQ, p.getUuid());
        pq.add(UserGroupPolicyRefVO_.groupUuid, Op.EQ, g.getUuid());
        UserGroupPolicyRefVO gref = pq.find();
        Assert.assertNotNull(gref);
    }
}
