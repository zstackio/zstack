package org.zstack.test.search;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestGenerateQuableFields {
    CLogger logger = Utils.getLogger(TestGenerateQuableFields.class);
    Api api;
    ComponentLoader loader;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.addXml("PortalForUnitTest.xml").addXml("AccountManager.xml").build();
        api = new Api();
        api.startServer();
    }


    @Test
    public void test() throws ApiSenderException {
        api.setTimeout(1000000);
        api.generateQueryableFields();
    }
}
