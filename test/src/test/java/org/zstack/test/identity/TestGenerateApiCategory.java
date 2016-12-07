package org.zstack.test.identity;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.GenerateMessageIdentityCategoryMsg;
import org.zstack.test.*;

/**
 */
public class TestGenerateApiCategory {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        con.addAllConfigInZstackXml();
        loader = con.build();
        dbf = loader.getComponent(DatabaseFacade.class);
        bus = loader.getComponent(CloudBus.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        GenerateMessageIdentityCategoryMsg msg = new GenerateMessageIdentityCategoryMsg();
        bus.makeLocalServiceId(msg, AccountConstant.SERVICE_ID);
        bus.call(msg);
    }
}
