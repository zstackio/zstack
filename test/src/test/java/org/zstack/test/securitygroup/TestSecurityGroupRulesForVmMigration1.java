package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleTO;
import org.zstack.simulator.SimulatorConfig;
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
 * @condition 1. create vm with sg rules
 * 2. migrate vm to another host
 * 3. make vm migration fail
 * @test confirm rules on vm on previous host are recovered
 * confirm rules on dest host are cleaned up
 */
public class TestSecurityGroupRulesForVmMigration1 {
    static CLogger logger = Utils.getLogger(TestSecurityGroupRulesForVmMigration1.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static SimulatorSecurityGroupBackend sbkd;
    static SimulatorConfig config;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestApplySeurityGroupRulesForVmMigration.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        sbkd = loader.getComponent(SimulatorSecurityGroupBackend.class);
        config = loader.getComponent(SimulatorConfig.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        HostInventory host2 = deployer.hosts.get("host2");
        SecurityGroupInventory scinv = deployer.securityGroups.get("test");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        VmNicInventory vm1Nic = vm1.getVmNics().get(0);
        api.addVmNicToSecurityGroup(scinv.getUuid(), vm1Nic.getUuid());
        TimeUnit.MILLISECONDS.sleep(500);

        String nicname = dbf.findByUuid(vm1Nic.getUuid(), VmNicVO.class).getInternalName();
        SecurityGroupRuleTO vmto = sbkd.getRulesOnHost(vm1.getHostUuid(), nicname);
        List<SecurityGroupRuleInventory> expectedRules = new ArrayList<SecurityGroupRuleInventory>();
        expectedRules.addAll(scinv.getRules());
        SecurityGroupTestValidator.validate(vmto, expectedRules);

        config.migrateSuccess = false;
        try {
            vm1 = api.migrateVmInstance(vm1.getUuid(), host2.getUuid());
        } catch (ApiSenderException e) {
        }

        TimeUnit.MILLISECONDS.sleep(1000);

        vmto = sbkd.getRulesOnHost(vm1.getHostUuid(), nicname);
        SecurityGroupTestValidator.validate(vmto, expectedRules);

        vmto = sbkd.getRulesOnHost(host2.getUuid(), nicname);
        Assert.assertNull(vmto);
    }
}
