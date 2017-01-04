package org.zstack.test.configuration;

import org.junit.Before;
import org.junit.Test;
import org.zstack.configuration.ConfigurationManagerImpl;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.query.MysqlQueryBuilderImpl3;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.image.TestAddImage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;

public class TestGenerateApiPythonClassAndJsonTemplate {
    CLogger logger = Utils.getLogger(TestAddImage.class);

    ComponentLoader loader;
    ConfigurationManagerImpl configurationManagerImpl;
    MysqlQueryBuilderImpl3 mysqlQueryBuilderImpl3;

    @Before
    public void setUp() throws Exception {
        //DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addAllConfigInZstackXml().build();

        mysqlQueryBuilderImpl3 = loader.getComponent(MysqlQueryBuilderImpl3.class);
        mysqlQueryBuilderImpl3.start();

        configurationManagerImpl = loader.getComponent(ConfigurationManagerImpl.class);
        configurationManagerImpl.start();
    }

    @Test
    public void test() throws ApiSenderException, IOException {
        configurationManagerImpl.generateApiJsonTemplate(null, null);
    }

}
