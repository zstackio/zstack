package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
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
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 *
 *
 */
public class TestGetCandidateVmNicForSecurityGroup1 {
    static CLogger logger = Utils.getLogger(TestGetCandidateVmNicForSecurityGroup1.class);
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

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.createAccount("test", "password");
        SessionInventory session = identityCreator.getAccountSession();
        SecurityGroupInventory sg3 = api.createSecurityGroup("name", session);
        List<VmNicInventory> nics1 = api.getCandidateVmNicFromSecurityGroup(sg3.getUuid(), session);
        Assert.assertEquals(0, nics1.size());

        api.shareResource(list(vm1Nic1.getUuid()), list(test.getUuid()), false);

        nics1 = api.getCandidateVmNicFromSecurityGroup(sg3.getUuid(), session);
        Assert.assertEquals(0, nics1.size());

        api.attachSecurityGroupToL3Network(sg3.getUuid(), l3nw1.getUuid());

        nics1 = api.getCandidateVmNicFromSecurityGroup(sg3.getUuid(), session);
        Assert.assertEquals(1, nics1.size());

        nics1 = api.getCandidateVmNicFromSecurityGroup(sg3.getUuid());
        Assert.assertEquals(2, nics1.size());

    }
}
