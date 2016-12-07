package org.zstack.test.storage.backup;

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
 * Time: 8:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestGetBackupStorageTypes {
    CLogger logger = Utils.getLogger(TestGetBackupStorageTypes.class);
    Api api;
    ComponentLoader loader;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("Simulator.xml")
                .addXml("BackupStorageManager.xml").addXml("ZoneManager.xml").addXml("AccountManager.xml").build();
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        List<String> types = api.getBackupStorageTypes();
        Assert.assertFalse(types.isEmpty());
    }
}
