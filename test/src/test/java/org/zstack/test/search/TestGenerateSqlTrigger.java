package org.zstack.test.search;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestGenerateSqlTrigger {
    CLogger logger = Utils.getLogger(TestGenerateSqlTrigger.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("SearchManager.xml").addXml("AccountManager.xml").build();
        api = new Api();
        api.startServer();
    }


    @Test
    public void test() throws ApiSenderException {
        api.generateSqlTrigger();
    }
}
