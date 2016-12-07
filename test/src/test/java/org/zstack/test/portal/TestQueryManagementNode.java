package org.zstack.test.portal;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.managementnode.APIQueryManagementNodeMsg;
import org.zstack.header.managementnode.APIQueryManagementNodeReply;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.test.*;
import org.zstack.test.search.QueryTestValidator;

import java.util.ArrayList;
import java.util.Map;

/**
 */
public class TestQueryManagementNode {
    ComponentLoader loader;
    Api api;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        con.addXml("PortalForUnitTest.xml");
        con.addXml("AccountManager.xml");
        loader = con.build();
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        ManagementNodeInventory inv = api.listManagementNodes().get(0);
        Assert.assertEquals(Platform.getManagementServerIp(), inv.getHostName());
        QueryTestValidator.validateEQ(new APIQueryManagementNodeMsg(), api, APIQueryManagementNodeReply.class, inv);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryManagementNodeMsg(), api, APIQueryManagementNodeReply.class, inv, 3);

        APIQueryManagementNodeMsg msg = new APIQueryManagementNodeMsg();
        msg.setConditions(new ArrayList<QueryCondition>());
        APIQueryManagementNodeReply reply = api.query(msg, APIQueryManagementNodeReply.class);
        Assert.assertEquals(1, reply.getInventories().size());

        String version = api.getVersion();
        Assert.assertNotNull(version);
        System.out.println(String.format("version: %s", version));

        Map<String, Long> currentTime = api.getCurrentTime();
        Assert.assertNotNull(currentTime.get("MillionSeconds"));
        Assert.assertNotNull(currentTime.get("Seconds"));
        System.out.println(String.format("current time in million seconds: %d", currentTime.get("MillionSeconds")));
    }
}
