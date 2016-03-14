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
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;

/**
 * 1. create a user
 * 2. create a policy
 * 3. attach the policy to the user
 * 4. delete the user
 *
 * confirm the policy is detached
 *
 * 5. create another user
 * 6. attach the policy to the user
 * 7. delete the policy
 *
 * confirm the policy is detached
 *
 * 8. create a group and a policy
 * 9. attach the policy to the group
 * 10. delete the group
 *
 * confirm the policy is detached
 *
 * 11. create another group and attach the policy
 * 12. delete the policy
 *
 * confirm the policy is detached
 *
 */
public class TestIdentity9 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
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

        int num = 10;
        List<String> policyUuids = new ArrayList<String>();
        for (int i=0; i<num; i++) {
            Statement s = new Statement();
            s.addAction(".*");
            s.setEffect(StatementEffect.Allow);
            PolicyInventory p = creator.createPolicy(String.format("test%s", i), s);
            policyUuids.add(p.getUuid());
        }

        creator.attachPoliciesToUser("test", policyUuids);
        SimpleQuery<UserPolicyRefVO> q = dbf.createQuery(UserPolicyRefVO.class);
        q.add(UserPolicyRefVO_.policyUuid, Op.IN, policyUuids);
        q.add(UserPolicyRefVO_.userUuid, Op.EQ, u.getUuid());
        List<UserPolicyRefVO> pvos = q.list();

        Assert.assertEquals(10, pvos.size());

        // operation idempotent
        creator.attachPoliciesToUser("test", policyUuids);
        pvos = q.list();
        Assert.assertEquals(10, pvos.size());

        creator.detachPoliciesFromUser("test", policyUuids);
        pvos = q.list();
        Assert.assertEquals(0, pvos.size());

        // operation idempotent
        creator.detachPoliciesFromUser("test", policyUuids);
        pvos = q.list();
        Assert.assertEquals(0, pvos.size());
    }
}
