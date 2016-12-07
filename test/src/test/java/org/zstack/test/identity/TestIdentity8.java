package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.*;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.test.*;

/**
 * 1. delete an account
 * <p>
 * confirm the resources under the account are deleted
 */
public class TestIdentity8 {
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
        AccountInventory a = creator.createAccount("test", "test");
        creator.createUser("user", "password");
        creator.createGroup("group");

        Statement s = new Statement();
        s.addAction(".*");
        s.setName("test");
        s.setEffect(StatementEffect.Allow);
        creator.createPolicy("test", s);

        api.deleteAccount(a.getUuid(), creator.getAccountSession());

        SimpleQuery<UserVO> uq = dbf.createQuery(UserVO.class);
        uq.add(UserVO_.accountUuid, Op.EQ, a.getUuid());
        Assert.assertFalse(uq.isExists());

        SimpleQuery<PolicyVO> pq = dbf.createQuery(PolicyVO.class);
        uq.add(PolicyVO_.accountUuid, Op.EQ, a.getUuid());
        Assert.assertFalse(pq.isExists());

        SimpleQuery<UserGroupVO> gq = dbf.createQuery(UserGroupVO.class);
        uq.add(UserGroupVO_.accountUuid, Op.EQ, a.getUuid());
        Assert.assertFalse(gq.isExists());
    }
}
