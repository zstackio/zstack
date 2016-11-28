package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleVO;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestAddSecurityGroupDuplicateRule2 {
    static CLogger logger = Utils.getLogger(TestAddSecurityGroupDuplicateRule2.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestAddSecurityGroupRule.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() {
        /*
            <rule>
				<type>Ingress</type>
				<protocol>TCP</protocol>
				<startPort>22</startPort>
				<endPort>100</endPort>
				<allowedCidr>0.0.0.0/0</allowedCidr>
			</rule>
            <rule>
				<type>Ingress</type>
				<protocol>UDP</protocol>
				<startPort>10</startPort>
				<endPort>10</endPort>
				<allowedCidr>192.168.0.1/0</allowedCidr>
			</rule>
         */
        //
        SecurityGroupInventory scinv = deployer.securityGroups.get("test");
        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO sao = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
        sao.setType("Ingress");
        sao.setProtocol("UDP");
        sao.setStartPort(10);
        sao.setEndPort(10);
        sao.setAllowedCidr("192.168.0.1/0");
        try {
            api.addSecurityGroupRuleByFullConfig(scinv.getUuid(), sao);
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
        //
        SecurityGroupInventory scinv1 = deployer.securityGroups.get("test");
        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO sao1 = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
        sao1.setType("Ingress");
        sao1.setProtocol("TCP");
        sao1.setStartPort(22);
        sao1.setEndPort(100);
        //sao1.setAllowedCidr("");
        try {
            api.addSecurityGroupRuleByFullConfig(scinv1.getUuid(), sao1);
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
        //
        long count = dbf.count(SecurityGroupRuleVO.class);
        Assert.assertEquals(2, count);
    }
}
