package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.WebBeanConstructor;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestGetL3NetworkTypes {
    Api api;
    ComponentLoader loader;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml")
                .addXml("NetworkManager.xml").addXml("AccountManager.xml").build();
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        List<String> types = api.getL3NetworkTypes();
        Assert.assertFalse(types.isEmpty());
    }
}
