package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.APIQueryAccountMsg;
import org.zstack.header.identity.APIQueryAccountReply;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.test.*;
import org.zstack.test.search.QueryTestValidator;

/**
 * 1. create an account
 * <p>
 * confirm the account created successfully
 */
public class TestIdentity1 {
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
        Assert.assertTrue(dbf.isExist(a.getUuid(), AccountVO.class));

        QueryTestValidator.validateEQ(new APIQueryAccountMsg(), api, APIQueryAccountReply.class, a);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryAccountMsg(), api, APIQueryAccountReply.class, a, 3);
    }
}
