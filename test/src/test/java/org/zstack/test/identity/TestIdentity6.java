package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.*;
import org.zstack.test.*;

import java.util.concurrent.TimeUnit;

/**
 * 1. create an account
 * 2. create a group
 * 3. create a user
 * 4. add the user to the group
 * <p>
 * confirm the all operations success
 * <p>
 * 5. remove the user from the group
 * <p>
 * confirm the user is not in the group
 * <p>
 * 5. add the user back to the group
 * 6. delete the user
 * <p>
 * confirm the user is not in the group
 * <p>
 * 7. create another user and add the user in the group
 * 8. delete the group
 * <p>
 * confirm the user is not in the group
 */
public class TestIdentity6 {
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
    public void test() throws ApiSenderException, InterruptedException {
        IdentityCreator creator = new IdentityCreator(api);
        AccountInventory a = creator.createAccount("test", "test");
        UserGroupInventory g = creator.createGroup("test");
        UserInventory u = creator.createUser("test", "test");
        creator.addUserToGroup("test", "test");

        UserGroupVO gvo = dbf.findByUuid(g.getUuid(), UserGroupVO.class);
        Assert.assertEquals(a.getUuid(), gvo.getAccountUuid());
        SimpleQuery<UserGroupUserRefVO> q = dbf.createQuery(UserGroupUserRefVO.class);
        q.add(UserGroupUserRefVO_.groupUuid, Op.EQ, g.getUuid());
        q.add(UserGroupUserRefVO_.userUuid, Op.EQ, u.getUuid());
        UserGroupUserRefVO ref = q.find();
        Assert.assertNotNull(ref);

        creator.removeUserFromGroup("test", "test");
        ref = q.find();
        Assert.assertNull(ref);

        creator.userLogin("test", "test");
        creator.addUserToGroup("test", "test");
        creator.deleteUser("test");
        Assert.assertFalse(dbf.isExist(u.getUuid(), UserVO.class));
        Assert.assertFalse(q.isExists());
        TimeUnit.SECONDS.sleep(2);
        SimpleQuery<SessionVO> sq = dbf.createQuery(SessionVO.class);
        sq.add(SessionVO_.userUuid, Op.EQ, u.getUuid());
        Assert.assertFalse(sq.isExists());

        UserInventory user = creator.createUser("user1", "password");
        creator.addUserToGroup("user1", "test");
        creator.deleteGroup("test");
        Assert.assertFalse(dbf.isExist(g.getUuid(), UserGroupVO.class));
        SimpleQuery<UserGroupUserRefVO> q1 = dbf.createQuery(UserGroupUserRefVO.class);
        q1.add(UserGroupUserRefVO_.groupUuid, Op.EQ, g.getUuid());
        q1.add(UserGroupUserRefVO_.userUuid, Op.EQ, user.getUuid());
        Assert.assertFalse(q1.isExists());
    }
}
