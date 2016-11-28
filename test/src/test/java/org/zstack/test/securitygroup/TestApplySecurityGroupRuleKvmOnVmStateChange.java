package org.zstack.test.securitygroup;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMConstant.KvmVmState;
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

import java.util.concurrent.TimeUnit;

/**
 * @author frank
 * @condition 1. create a security group with some rules
 * 2. create a vm and add vm to the security group
 * 3. put vm to Unknown state
 * 4. put vm back to Running state
 * @test confirm rules on vm are correct
 * @deprecated this case is not valid any more
 */
public class TestApplySecurityGroupRuleKvmOnVmStateChange {
    static CLogger logger = Utils.getLogger(TestApplySecurityGroupRuleKvmOnVmStateChange.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static KVMSimulatorConfig config;
    static GlobalConfigFacade gcf;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestApplySeurityGroupRulesToVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        loader = deployer.getComponentLoader();
        gcf = loader.getComponent(GlobalConfigFacade.class);
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
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
        VmNicInventory vmNic = vm.getVmNics().get(0);

        config.securityGroupSuccess = true;
        api.addVmNicToSecurityGroup(scinv.getUuid(), vmNic.getUuid());
        TimeUnit.MILLISECONDS.sleep(500);
        config.securityGroups.clear();
        config.vms.remove(vm.getUuid());
        TimeUnit.SECONDS.sleep(2);
        config.vms.put(vm.getUuid(), KvmVmState.Running);
        TimeUnit.SECONDS.sleep(5);

        SecurityGroupRuleTO to = config.securityGroups.get(vmNic.getInternalName());
        SecurityGroupTestValidator.validate(to, scinv.getRules());
    }
}
