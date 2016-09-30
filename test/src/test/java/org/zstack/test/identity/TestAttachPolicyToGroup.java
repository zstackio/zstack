package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.APIQueryPolicyMsg;
import org.zstack.header.identity.APIQueryPolicyReply;
import org.zstack.header.identity.PolicyInventory;
import org.zstack.header.identity.UserGroupInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestAttachPolicyToGroup {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/identity/TestAttachPolicyToGroup.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        UserGroupInventory group = deployer.groups.get("TestGroup1");
        APIQueryPolicyMsg msg = new APIQueryPolicyMsg();
        msg.addQueryCondition("group.uuid", QueryOp.EQ, group.getUuid());
        APIQueryPolicyReply reply = api.query(msg, APIQueryPolicyReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        PolicyInventory p = reply.getInventories().get(0);
        Assert.assertEquals("TestPolicy", p.getName());
    }
}
