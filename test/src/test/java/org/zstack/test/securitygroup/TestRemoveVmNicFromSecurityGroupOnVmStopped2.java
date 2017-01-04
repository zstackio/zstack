package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.L3NetworkInventory;
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
 * @condition 1. create a security group: sg and attach to l3network
 * 2. create vm1 and vm1Nic1 to sg
 * 3. stop vm
 * 4. remove vm1Nic1 from sg
 * 5. start vm
 * @test confirm after rules for vm1Nic1 were removed
 */
public class TestRemoveVmNicFromSecurityGroupOnVmStopped2 {
    static CLogger logger = Utils.getLogger(TestRemoveVmNicFromSecurityGroupOnVmStopped2.class);
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
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        L3NetworkInventory l3nw1 = deployer.l3Networks.get("TestL3Network1");
        VmNicInventory vm1Nic1 = SecurityGroupTestValidator.getVmNicOnSpecificL3Network(vm1.getVmNics(), l3nw1.getUuid());
        String nicName = dbf.findByUuid(vm1Nic1.getUuid(), VmNicVO.class).getInternalName();

        api.addVmNicToSecurityGroup(scinv.getUuid(), vm1Nic1.getUuid());

        TimeUnit.MILLISECONDS.sleep(500);
        SecurityGroupRuleTO vm1Nic1TO = sbkd.getRulesOnHost(vm1.getHostUuid(), nicName);
        SecurityGroupTestValidator.validate(vm1Nic1TO, scinv.getRules());

        api.stopVmInstance(vm1.getUuid());
        api.removeVmNicFromSecurityGroup(scinv.getUuid(), vm1Nic1.getUuid());
        api.startVmInstance(vm1.getUuid());
        TimeUnit.MILLISECONDS.sleep(500);

        vm1Nic1TO = sbkd.getRulesOnHost(vm1.getHostUuid(), nicName);
        Assert.assertTrue(vm1Nic1TO.getRules().isEmpty());
    }
}
