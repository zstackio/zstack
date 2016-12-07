package org.zstack.test.storage.primary;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.WebBeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 12:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestGetPrimaryStoragesType {
    CLogger logger = Utils.getLogger(TestGetPrimaryStoragesType.class);
    Api api;
    ComponentLoader loader;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("Simulator.xml").addXml("PrimaryStorageManager.xml")
                .addXml("ZoneManager.xml").addXml("ClusterManager.xml").addXml("ConfigurationManager.xml").addXml("AccountManager.xml").build();
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        List<String> types = api.getPrimaryStorageTypes();
        Assert.assertFalse(types.isEmpty());
        System.out.println(types);
    }
}
