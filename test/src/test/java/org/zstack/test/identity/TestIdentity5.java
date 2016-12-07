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
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;

public class TestIdentity5 {
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

        QueryTestValidator.validateEQ(new APIQueryPolicyMsg(), api, APIQueryPolicyReply.class, p);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryPolicyMsg(), api, APIQueryPolicyReply.class, p, 3);
    }
}
