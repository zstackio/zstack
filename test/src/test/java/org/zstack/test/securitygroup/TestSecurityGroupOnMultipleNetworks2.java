package org.zstack.test.securitygroup;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleProtocolType;
import org.zstack.network.securitygroup.SecurityGroupRuleTO;
import org.zstack.network.securitygroup.SecurityGroupRuleType;
import org.zstack.simulator.SimulatorSecurityGroupBackend;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * @author frank
 * @condition 1. create a security group: sg
 * 2. attach sg to two l3Networks: l3nw1, l3nw2
 * 3. create vm1 with two nics: vm1Nic1(l3nw1), vm1Nic2(l3nw2)
 * 4. add vm1Nic1 to sg
 * 5. create vm2 with two nics: vm2Nic1(l3nw1), vm2Nic2(l3nw2)
 * 6. add vm2Nic2 to sg
 * 7. verify rules on both nics
 * 8. add a new rule to sg
 * @test confirm after adding new rule, rules on vm1Nic1 and vm2Nic2 are updated
 */
public class TestSecurityGroupOnMultipleNetworks2 {
    static CLogger logger = Utils.getLogger(TestSecurityGroupOnMultipleNetworks2.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static SimulatorSecurityGroupBackend sbkd;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestSeurityGroupOnMultipleNetworks.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        sbkd = loader.getComponent(SimulatorSecurityGroupBackend.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SecurityGroupInventory scinv = deployer.securityGroups.get("test");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        L3NetworkInventory l3nw1 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory l3nw2 = deployer.l3Networks.get("TestL3Network2");
        VmNicInventory vm1Nic1 = SecurityGroupTestValidator.getVmNicOnSpecificL3Network(vm1.getVmNics(), l3nw1.getUuid());

        VmInstanceInventory vm2 = deployer.vms.get("TestVm1");
        VmNicInventory vm2Nic2 = SecurityGroupTestValidator.getVmNicOnSpecificL3Network(vm2.getVmNics(), l3nw2.getUuid());

        api.addVmNicToSecurityGroup(scinv.getUuid(), vm1Nic1.getUuid());
        api.addVmNicToSecurityGroup(scinv.getUuid(), vm2Nic2.getUuid());

        TimeUnit.MILLISECONDS.sleep(500);
        SecurityGroupRuleTO vm1Nic1TO = sbkd.getRulesOnHost(vm1.getHostUuid(), vm1Nic1.getInternalName());
        SecurityGroupTestValidator.validate(vm1Nic1TO, scinv.getRules());
        SecurityGroupRuleTO vm2Nic2TO = sbkd.getRulesOnHost(vm2.getHostUuid(), vm2Nic2.getInternalName());
        SecurityGroupTestValidator.validate(vm2Nic2TO, scinv.getRules());

        SecurityGroupRuleAO ao = new SecurityGroupRuleAO();
        ao.setAllowedCidr("87.99.11.1/32");
        ao.setStartPort(999);
        ao.setEndPort(10000);
        ao.setProtocol(SecurityGroupRuleProtocolType.TCP.toString());
        ao.setType(SecurityGroupRuleType.Ingress.toString());
        scinv = api.addSecurityGroupRuleByFullConfig(scinv.getUuid(), ao);
        TimeUnit.MILLISECONDS.sleep(500);

        vm1Nic1TO = sbkd.getRulesOnHost(vm1.getHostUuid(), vm1Nic1.getInternalName());
        SecurityGroupTestValidator.validate(vm1Nic1TO, scinv.getRules());
        vm2Nic2TO = sbkd.getRulesOnHost(vm2.getHostUuid(), vm2Nic2.getInternalName());
        SecurityGroupTestValidator.validate(vm2Nic2TO, scinv.getRules());
    }
}
