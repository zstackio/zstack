package org.zstack.test.securitygroup;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
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
 * @author root
 * @condition 1. create two vms: vm1, vm2
 * 2. each vm has two nics: vm1Nic1, vm1Nic2, vm2Nic1, vm2Nic2
 * 3. create two security groups with some rules: sg1,sg2
 * 4. add vm1Nic1, vm2Nic1 to sg1
 * 5. add vm1Nic2, vm2Nic2 to sg2
 * @test confirm each vm can reach each other
 */
public class TestApplySecurityGroupRuleToVmOnKvm2 {
    static CLogger logger = Utils.getLogger(TestApplySecurityGroupRuleToVmOnKvm2.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static KVMSimulatorConfig config;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestApplySeurityGroupRulesToVmOnKvm2.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
    }

    private VmNicInventory getNicByL3NwUuid(String vmUuid, String l3NwUuid) {
        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vmUuid)
                .eq(VmNicVO_.l3NetworkUuid, l3NwUuid).find();
        return VmNicInventory.valueOf(nic);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SecurityGroupInventory scinv1 = deployer.securityGroups.get("test1");
        SecurityGroupInventory scinv2 = deployer.securityGroups.get("test2");
        L3NetworkInventory l3nw1 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory l3nw2 = deployer.l3Networks.get("TestL3Network2");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        VmNicInventory vm1Nic1 = getNicByL3NwUuid(vm1.getUuid(), l3nw1.getUuid());
        VmNicInventory vm1Nic2 = getNicByL3NwUuid(vm1.getUuid(), l3nw2.getUuid());
        VmNicInventory vm2Nic1 = getNicByL3NwUuid(vm2.getUuid(), l3nw1.getUuid());
        VmNicInventory vm2Nic2 = getNicByL3NwUuid(vm2.getUuid(), l3nw2.getUuid());

        config.securityGroupSuccess = true;
        // add to security group 1
        List<String> nicUuids = new ArrayList<String>();
        nicUuids.add(vm1Nic1.getUuid());
        nicUuids.add(vm2Nic1.getUuid());
        api.addVmNicToSecurityGroup(scinv1.getUuid(), nicUuids);

        // add to security group 2
        nicUuids.clear();
        nicUuids.add(vm1Nic2.getUuid());
        nicUuids.add(vm2Nic2.getUuid());
        api.addVmNicToSecurityGroup(scinv2.getUuid(), nicUuids);

        TimeUnit.MILLISECONDS.sleep(500);
        SecurityGroupRuleTO actual11 = config.securityGroups.get(vm1Nic1.getInternalName());
        SecurityGroupRuleTO actual21 = config.securityGroups.get(vm2Nic1.getInternalName());
        SecurityGroupRuleTO actual12 = config.securityGroups.get(vm1Nic2.getInternalName());
        SecurityGroupRuleTO actual22 = config.securityGroups.get(vm1Nic2.getInternalName());
        SecurityGroupTestValidator.validateInternalIpIn(actual11, vm2Nic1.getIp(), scinv1.getRules());
        SecurityGroupTestValidator.validateInternalIpIn(actual21, vm1Nic1.getIp(), scinv1.getRules());
        SecurityGroupTestValidator.validateInternalIpIn(actual12, vm2Nic2.getIp(), scinv2.getRules());
        SecurityGroupTestValidator.validateInternalIpIn(actual22, vm1Nic2.getIp(), scinv2.getRules());
    }
}
