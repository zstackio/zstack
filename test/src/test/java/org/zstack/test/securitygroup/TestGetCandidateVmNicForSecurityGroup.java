package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.simulator.SimulatorSecurityGroupBackend;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * @author frank
 * @condition 1. create a security group: sg
 * 2. attach sg to two l3Networks: l3nw1, l3nw2
 * 3. create vm1 with two nics: vm1Nic1(l3nw1), vm1Nic2(l3nw2)
 * 5. create vm2 with two nics: vm2Nic1(l3nw1), vm2Nic2(l3nw2)
 * @test confirm APIGetCandidateVmNicForSecurityGroupMsg returns correct nics
 */
public class TestGetCandidateVmNicForSecurityGroup {
    static CLogger logger = Utils.getLogger(TestGetCandidateVmNicForSecurityGroup.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static SimulatorSecurityGroupBackend sbkd;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestSeurityGroupOnMultipleNetworks.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        sbkd = loader.getComponent(SimulatorSecurityGroupBackend.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        L3NetworkInventory l3nw1 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory l3nw2 = deployer.l3Networks.get("TestL3Network2");
        VmNicInventory vm1Nic1 = SecurityGroupTestValidator.getVmNicOnSpecificL3Network(vm1.getVmNics(), l3nw1.getUuid());
        VmNicInventory vm1Nic2 = SecurityGroupTestValidator.getVmNicOnSpecificL3Network(vm1.getVmNics(), l3nw2.getUuid());

        VmInstanceInventory vm2 = deployer.vms.get("TestVm1");
        VmNicInventory vm2Nic1 = SecurityGroupTestValidator.getVmNicOnSpecificL3Network(vm2.getVmNics(), l3nw1.getUuid());
        VmNicInventory vm2Nic2 = SecurityGroupTestValidator.getVmNicOnSpecificL3Network(vm2.getVmNics(), l3nw2.getUuid());

        SecurityGroupInventory sg1 = deployer.securityGroups.get("test");
        SecurityGroupInventory sg2 = deployer.securityGroups.get("test1");

        List<VmNicInventory> nics1 = api.getCandidateVmNicFromSecurityGroup(sg1.getUuid());
        Assert.assertEquals(4, nics1.size());
        List<VmNicInventory> nics2 = api.getCandidateVmNicFromSecurityGroup(sg2.getUuid());
        Assert.assertEquals(2, nics2.size());
        for (VmNicInventory nic : nics2) {
            if (!nic.getUuid().equals(vm1Nic1.getUuid()) && !nic.getUuid().equals(vm2Nic1.getUuid())) {
                Assert.fail(nic.getUuid());
            }
        }

        api.addVmNicToSecurityGroup(sg2.getUuid(), vm1Nic1.getUuid());
        nics2 = api.getCandidateVmNicFromSecurityGroup(sg2.getUuid());
        Assert.assertEquals(1, nics2.size());
        VmNicInventory n = nics2.get(0);
        Assert.assertEquals(vm2Nic1.getUuid(), n.getUuid());
    }
}
