package org.zstack.test.compute.hostallocator;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestGetHostAllocatorStrategies {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/hostAllocator/TestHostAllocator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
    }

    @Test
    public void test() throws ApiSenderException {
        List<String> types = api.getHostAllocatorStrategies();
        Assert.assertFalse(types.isEmpty());
    }
}
