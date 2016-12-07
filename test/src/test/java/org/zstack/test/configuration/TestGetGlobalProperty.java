package org.zstack.test.configuration;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.APIGetGlobalPropertyMsg;
import org.zstack.header.configuration.APIGetGlobalPropertyReply;
import org.zstack.test.*;
import org.zstack.test.image.TestAddImage;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

public class TestGetGlobalProperty {
    CLogger logger = Utils.getLogger(TestAddImage.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addAllConfigInZstackXml().build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        APIGetGlobalPropertyMsg msg = new APIGetGlobalPropertyMsg();
        msg.setSession(api.getAdminSession());
        ApiSender sender = api.getApiSender();
        APIGetGlobalPropertyReply reply = sender.call(msg, APIGetGlobalPropertyReply.class);
        System.out.println(JSONObjectUtil.dumpPretty(reply.getProperties()));
    }

}
