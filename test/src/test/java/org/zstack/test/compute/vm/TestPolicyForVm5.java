package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. share resources to another account
 * 2. create a vm by the account
 * <p>
 * confirm the virtual router is owned by the original account
 */
public class TestPolicyForVm5 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/vm/TestPolicyForVm5.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }


    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory l3 = deployer.l3Networks.get("GuestNetwork");
        ImageInventory image = deployer.images.get("TestImage");
        InstanceOfferingInventory vroffering = deployer.instanceOfferings.get("virtualRouterOffering");
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory account2 = identityCreator.createAccount("account2", "password");

        AccountInventory account1 = deployer.accounts.get("test");
        SessionInventory session1 = api.loginByAccount("test", "password");

        api.shareResource(list(image.getUuid(), ioinv.getUuid(), l3.getUuid(), vroffering.getUuid()),
                list(account2.getUuid()), false, session1);

        VmCreator vmCreator = new VmCreator(api);
        vmCreator.name = "name";
        vmCreator.instanceOfferingUuid = ioinv.getUuid();
        vmCreator.imageUuid = image.getUuid();
        vmCreator.addL3Network(l3.getUuid());
        vmCreator.session = identityCreator.getAccountSession();
        vmCreator.create();

        api.deleteAccount(account2.getUuid(), vmCreator.session);

        VirtualRouterVmVO vr = dbf.listAll(VirtualRouterVmVO.class).get(0);

        SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
        q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, vr.getUuid());
        AccountResourceRefVO ref = q.find();
        Assert.assertEquals(ref.getAccountUuid(), account1.getUuid());
    }
}


