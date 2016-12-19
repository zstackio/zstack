package org.zstack.test.configuration;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.image.TestAddImage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestGenerateApiPythonClassAndJsonTemplateWithRunningNode {
    CLogger logger = Utils.getLogger(TestAddImage.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        //DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addAllConfigInZstackXml().build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        api.setTimeout(1200);
        api.generateApiJsonTemplate();
    }

}
