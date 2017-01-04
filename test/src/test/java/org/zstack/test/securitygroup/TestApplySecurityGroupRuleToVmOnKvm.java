package org.zstack.test.securitygroup;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.securitygroup.SecurityGroupInventory;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author frank
 * @condition 1. create a security group
 * 2. add some rules
 * 3. create a vm
 * 4. add one nic of vm to security group
 */
public class TestApplySecurityGroupRuleToVmOnKvm {
    static CLogger logger = Utils.getLogger(TestApplySecurityGroupRuleToVmOnKvm.class);
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
        List<String> nicNames = new ArrayList<String>();
        String nicUuid = vm.getVmNics().get(0).getUuid();
        String nicName = Q.New(VmNicVO.class).select(VmNicVO_.internalName).eq(VmNicVO_.uuid, nicUuid).findValue();
        nicNames.add(nicName);

        List<String> nicUuids = new ArrayList<String>();
        nicUuids.add(vm.getVmNics().get(0).getUuid());

        config.securityGroupSuccess = true;
        api.addVmNicToSecurityGroup(scinv.getUuid(), nicUuids);
        TimeUnit.MILLISECONDS.sleep(500);

        SecurityGroupRuleTO to = config.securityGroups.get(nicName);
        SecurityGroupTestValidator.validate(to, scinv.getRules());
    }
}
