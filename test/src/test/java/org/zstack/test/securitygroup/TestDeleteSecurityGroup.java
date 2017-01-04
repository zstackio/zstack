package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleTO;
import org.zstack.network.securitygroup.SecurityGroupVO;
import org.zstack.simulator.SimulatorSecurityGroupBackend;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author frank
 * @condition 1. create security group with some rules
 * 2. create a vm and add into security group
 * 3. delete security group
 * @test confirm no security group rules on host anymore
 */
public class TestDeleteSecurityGroup {
    static CLogger logger = Utils.getLogger(TestDeleteSecurityGroup.class);
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

        api.addVmNicToSecurityGroup(scinv.getUuid(), vm.getVmNics().get(0).getUuid());
        TimeUnit.MILLISECONDS.sleep(500);
        api.deleteSecurityGroup(scinv.getUuid());
        TimeUnit.MILLISECONDS.sleep(500);

        String nicName = dbf.findByUuid(vm.getVmNics().get(0).getUuid(), VmNicVO.class).getInternalName();

        Set<SecurityGroupRuleTO> tos = sbkd.getRulesOnHost(vm.getHostUuid());
        SecurityGroupRuleTO rto = null;
        for (SecurityGroupRuleTO to : tos) {
            if (to.getVmNicInternalName().equals(nicName)) {
                rto = to;
                break;
            }
        }

        Assert.assertEquals(0, rto.getRules().size());

        AccountReferenceValidator referenceValidator = new AccountReferenceValidator();
        referenceValidator.noReference(scinv.getUuid(), SecurityGroupVO.class);
    }
}
