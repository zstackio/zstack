package org.zstack.test.securitygroup;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleTO;
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
 * @condition 1. create two security groups: sg1, sg2
 * 2. create two vms: vm1, vm2
 * 3. add vm1 to sg1, vm2 to sg2
 * @test confirm rules on both vms are correct
 */
public class TestSecurityGroupRuleInTwoGroup3 {
    static CLogger logger = Utils.getLogger(TestSecurityGroupRuleInTwoGroup3.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static SimulatorSecurityGroupBackend sbkd;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestApplySeurityGroupRulesInTwoGroup.xml", con);
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
        VmNicInventory vm1Nic = vm1.getVmNics().get(0);

        api.addVmNicToSecurityGroup(scinv.getUuid(), vm1Nic.getUuid());

        SecurityGroupInventory scinv2 = deployer.securityGroups.get("test1");

        VmInstanceInventory vm2 = deployer.vms.get("TestVm1");
        VmNicInventory vm2Nic = vm2.getVmNics().get(0);
        api.addVmNicToSecurityGroup(scinv2.getUuid(), vm2Nic.getUuid());

        TimeUnit.MILLISECONDS.sleep(500);

        VmNicVO vm1NicVO = dbf.findByUuid(vm1Nic.getUuid(), VmNicVO.class);
        SecurityGroupRuleTO tovm1 = sbkd.getRulesOnHost(vm1.getHostUuid(), vm1NicVO.getInternalName());
        SecurityGroupTestValidator.validate(tovm1, scinv.getRules());
        VmNicVO vm2NicVO = dbf.findByUuid(vm2Nic.getUuid(), VmNicVO.class);
        SecurityGroupRuleTO tovm2 = sbkd.getRulesOnHost(vm2.getHostUuid(), vm2NicVO.getInternalName());
        SecurityGroupTestValidator.validate(tovm2, scinv2.getRules());
    }
}
