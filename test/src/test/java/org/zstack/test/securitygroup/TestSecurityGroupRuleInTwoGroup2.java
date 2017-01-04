package org.zstack.test.securitygroup;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO;
import org.zstack.network.securitygroup.*;
import org.zstack.simulator.SimulatorSecurityGroupBackend;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author frank
 * @condition 1. create two security groups with some rules
 * 2. create a vm, add to both security groups
 * 3. after vm added, add one more rules to both security group
 * @test confirm rules on vm are correct
 */
public class TestSecurityGroupRuleInTwoGroup2 {
    static CLogger logger = Utils.getLogger(TestSecurityGroupRuleInTwoGroup2.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static SimulatorSecurityGroupBackend sbkd;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestApplySeurityGroupRulesInTwoGroup2.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        sbkd = loader.getComponent(SimulatorSecurityGroupBackend.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SecurityGroupInventory scinv = deployer.securityGroups.get("test");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory vmNic = vm.getVmNics().get(0);

        api.addVmNicToSecurityGroup(scinv.getUuid(), vmNic.getUuid());

        SecurityGroupInventory scinv2 = deployer.securityGroups.get("test1");
        api.addVmNicToSecurityGroup(scinv2.getUuid(), vmNic.getUuid());

        SecurityGroupRuleAO rule = new SecurityGroupRuleAO();
        rule.setAllowedCidr("192.168.1.10/32");
        rule.setEndPort(100);
        rule.setStartPort(20);
        rule.setProtocol(SecurityGroupRuleProtocolType.TCP.toString());
        rule.setType(SecurityGroupRuleType.Ingress.toString());
        List<SecurityGroupRuleAO> aos = new ArrayList<SecurityGroupRuleAO>();
        aos.add(rule);

        api.addSecurityGroupRuleByFullConfig(scinv.getUuid(), aos);

        rule = new SecurityGroupRuleAO();
        rule.setAllowedCidr("192.168.0.0/24");
        rule.setEndPort(200);
        rule.setStartPort(100);
        rule.setProtocol(SecurityGroupRuleProtocolType.UDP.toString());
        rule.setType(SecurityGroupRuleType.Egress.toString());
        aos.clear();
        aos.add(rule);

        api.addSecurityGroupRuleByFullConfig(scinv2.getUuid(), aos);
        TimeUnit.MILLISECONDS.sleep(500);

        String nicName = dbf.findByUuid(vmNic.getUuid(), VmNicVO.class).getInternalName();
        SecurityGroupRuleTO to = sbkd.getRulesOnHost(vm.getHostUuid(), nicName);

        List<SecurityGroupInventory> sgs = api.listSecurityGroup(null);
        List<SecurityGroupRuleInventory> expectedRules = new ArrayList<SecurityGroupRuleInventory>();
        for (SecurityGroupInventory sg : sgs) {
            expectedRules.addAll(sg.getRules());
        }

        SecurityGroupTestValidator.validate(to, expectedRules);
    }
}
