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
 * 2. create vm1
 * 3. add vm1 to sg1, sg2
 * 4. remove sg2
 * @test confirm rules of sg1 are still on vm1
 */
public class TestSecurityGroupRuleInTwoGroup4 {
    static CLogger logger = Utils.getLogger(TestSecurityGroupRuleInTwoGroup4.class);
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

        api.addVmNicToSecurityGroup(scinv2.getUuid(), vm1Nic.getUuid());

        TimeUnit.MILLISECONDS.sleep(500);

        api.deleteSecurityGroup(scinv2.getUuid());
        TimeUnit.MILLISECONDS.sleep(500);
        VmNicVO vm1NicVO = dbf.findByUuid(vm1Nic.getUuid(), VmNicVO.class);
        SecurityGroupRuleTO tovm1 = sbkd.getRulesOnHost(vm1.getHostUuid(), vm1NicVO.getInternalName());
        SecurityGroupTestValidator.validate(tovm1, scinv.getRules());
    }
}
