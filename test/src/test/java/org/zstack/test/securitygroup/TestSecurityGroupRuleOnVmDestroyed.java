package org.zstack.test.securitygroup;

import junit.framework.Assert;
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
 * @condition 1. create a security group
 * 2. create a vm and add it to security group
 * 3. destroy vm
 * @test confirm rules are cleaned on vm
 */
public class TestSecurityGroupRuleOnVmDestroyed {
    static CLogger logger = Utils.getLogger(TestSecurityGroupRuleOnVmDestroyed.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static SimulatorSecurityGroupBackend sbkd;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestApplySeurityGroupRulesToVmOnSimulator.xml", con);
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
        TimeUnit.MILLISECONDS.sleep(500);
        api.destroyVmInstance(vm.getUuid());
        TimeUnit.MILLISECONDS.sleep(500);

        VmNicVO nicvo = dbf.findByUuid(vmNic.getUuid(), VmNicVO.class);
        SecurityGroupRuleTO to = sbkd.getRulesOnHost(vm.getHostUuid(), nicvo.getInternalName());
        Assert.assertEquals(0, to.getRules().size());
    }
}
