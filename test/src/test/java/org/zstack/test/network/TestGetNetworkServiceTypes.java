package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.test.Api;
import org.zstack.test.BeanConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestGetNetworkServiceTypes {
    Api api;
    ComponentLoader loader;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.addAllConfigInZstackXml().build();
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws Exception {
        Map<String, List<String>> ret = api.getNetworkServiceTypes();
        Assert.assertFalse(ret.isEmpty());
    }
}
