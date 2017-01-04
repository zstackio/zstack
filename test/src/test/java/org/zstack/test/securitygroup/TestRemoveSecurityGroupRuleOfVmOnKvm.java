package org.zstack.test.securitygroup;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleTO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author frank
 * @condition 1. create a security group with some rules
 * 2. create a vm and add one nic in security group
 * 3. remove a rule from security group
 * @test confirm the rule was removed from vm
 */
public class TestRemoveSecurityGroupRuleOfVmOnKvm {
    static CLogger logger = Utils.getLogger(TestRemoveSecurityGroupRuleOfVmOnKvm.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static KVMSimulatorConfig config;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestApplySeurityGroupRulesToVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SecurityGroupInventory scinv = deployer.securityGroups.get("test");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);

        List<String> nicUuids = new ArrayList<String>();
        nicUuids.add(vm.getVmNics().get(0).getUuid());
        Map<String, List<String>> vmAndNicUuids = new HashMap<String, List<String>>();
        vmAndNicUuids.put(vm.getUuid(), nicUuids);

        config.securityGroupSuccess = true;
        api.addVmNicToSecurityGroup(scinv.getUuid(), nic.getUuid());
        TimeUnit.MILLISECONDS.sleep(500);

        SecurityGroupRuleInventory ruleToRemove = scinv.getRules().get(0);
        List<String> rsUuid = new ArrayList<String>(1);
        rsUuid.add(ruleToRemove.getUuid());
        api.removeSecurityGroupRule(rsUuid);
        TimeUnit.MILLISECONDS.sleep(500);
        scinv.getRules().remove(ruleToRemove);

        SecurityGroupRuleTO to = config.securityGroups.get(dbf.findByUuid(nic.getUuid(), VmNicVO.class).getInternalName());
        SecurityGroupTestValidator.validate(to, scinv.getRules());
    }
}
