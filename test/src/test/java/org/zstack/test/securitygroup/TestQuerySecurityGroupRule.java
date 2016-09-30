package org.zstack.test.securitygroup;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.network.securitygroup.APIQuerySecurityGroupRuleMsg;
import org.zstack.network.securitygroup.APIQuerySecurityGroupRuleReply;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestQuerySecurityGroupRule {
    static CLogger logger = Utils.getLogger(TestQuerySecurityGroupRule.class);
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
        SecurityGroupRuleInventory rule = inv.getRules().get(0);
        QueryTestValidator.validateEQ(new APIQuerySecurityGroupRuleMsg(), api, APIQuerySecurityGroupRuleReply.class, rule, session);
        QueryTestValidator.validateRandomEQConjunction(new APIQuerySecurityGroupRuleMsg(), api, APIQuerySecurityGroupRuleReply.class, rule, session, 2);
    }
}
