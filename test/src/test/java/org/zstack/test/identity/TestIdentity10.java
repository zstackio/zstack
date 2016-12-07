package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.*;
import org.zstack.test.*;

/**
 * test update identity
 */
public class TestIdentity10 {
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
        UserInventory user = creator.createUser("user", "password");
        UserGroupInventory group = creator.createGroup("group");

        a.setName("account");
        a.setDescription("account description");
        a = api.updateAccount(a, "password1", creator.accountSession);
        Assert.assertEquals("account", a.getName());
        Assert.assertEquals("account description", a.getDescription());
        AccountVO avo = dbf.findByUuid(a.getUuid(), AccountVO.class);
        Assert.assertEquals("password1", avo.getPassword());

        api.updateAccount(a, "password2", null);
        avo = dbf.findByUuid(a.getUuid(), AccountVO.class);
        Assert.assertEquals("password2", avo.getPassword());

        SessionInventory userSession = creator.userLogin("user", "password");
        user.setName("user1");
        user.setDescription("user description");
        user = api.updateUser(user, "password1", userSession);
        Assert.assertEquals("user1", user.getName());
        Assert.assertEquals("user description", user.getDescription());

        UserVO uservo = dbf.findByUuid(user.getUuid(), UserVO.class);
        Assert.assertEquals("password1", uservo.getPassword());

        user = api.updateUser(user, "password2", creator.getAccountSession());
        uservo = dbf.findByUuid(user.getUuid(), UserVO.class);
        Assert.assertEquals("password2", uservo.getPassword());

        user = api.updateUser(user, "password3", null);
        uservo = dbf.findByUuid(user.getUuid(), UserVO.class);
        Assert.assertEquals("password3", uservo.getPassword());

        group.setName("group1");
        group.setDescription("group description");
        group = api.updateUserGroup(group, creator.accountSession);
        Assert.assertEquals("group1", group.getName());
        Assert.assertEquals("group description", group.getDescription());

        group.setName("group2");
        group = api.updateUserGroup(group, null);
        Assert.assertEquals("group2", group.getName());
    }
}
