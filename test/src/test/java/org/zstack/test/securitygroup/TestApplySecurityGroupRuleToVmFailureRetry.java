package org.zstack.test.securitygroup;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.securitygroup.SecurityGroupGlobalConfig;
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

public class TestApplySecurityGroupRuleToVmFailureRetry {
    static CLogger logger = Utils.getLogger(TestApplySecurityGroupRuleToVmFailureRetry.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static GlobalConfigFacade gcf;
    static KVMSimulatorConfig config;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestApplySeurityGroupRulesToVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        SecurityGroupGlobalConfig.FAILURE_HOST_WORKER_INTERVAL.updateValue(1);
        deployer.build();
        api = deployer.getApi();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SecurityGroupInventory scinv = deployer.securityGroups.get("test");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);
        VmNicVO vmNic = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
        config.securityGroupSuccess = false;
        api.addVmNicToSecurityGroup(scinv.getUuid(), vmNic.getUuid());
        TimeUnit.MILLISECONDS.sleep(500);
        config.securityGroupSuccess = true;
        //TimeUnit.SECONDS.sleep(500);
        TimeUnit.SECONDS.sleep(5);

        SecurityGroupRuleTO to = config.securityGroups.get(vmNic.getInternalName());
        SecurityGroupTestValidator.validate(to, scinv.getRules());
    }
}
