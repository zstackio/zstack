package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.network.securitygroup.APIQuerySecurityGroupMsg;
import org.zstack.network.securitygroup.APIQuerySecurityGroupReply;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestQuerySecurityGroup {
    static CLogger logger = Utils.getLogger(TestQuerySecurityGroup.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestQuerySecurityGroup.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        SessionInventory session = api.loginByAccount("TestAccount", "password");
        SecurityGroupInventory inv = deployer.securityGroups.get("test");
        QueryTestValidator.validateEQ(new APIQuerySecurityGroupMsg(), api, APIQuerySecurityGroupReply.class, inv, session);
        QueryTestValidator.validateRandomEQConjunction(new APIQuerySecurityGroupMsg(), api, APIQuerySecurityGroupReply.class, inv, session, 2);

        L3NetworkInventory l3inv = deployer.l3Networks.get("TestL3Network1");
        APIQuerySecurityGroupMsg msg = new APIQuerySecurityGroupMsg();
        msg.addQueryCondition("attachedL3NetworkUuids", QueryOp.NOT_EQ, l3inv.getUuid());
        APIQuerySecurityGroupReply reply = api.query(msg, APIQuerySecurityGroupReply.class, session);
        Assert.assertTrue(reply.getInventories().isEmpty());
    }
}
