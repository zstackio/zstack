package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.UserInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

/**
 * 1. create a user
 * 2. assign no permission to the user
 * <p>
 * confirm the user can not create/start/stop/reboot/destroy/migrate the vm
 */
public class TestPolicyForVm1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestPolicyForVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory img = deployer.images.get("TestImage");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        HostInventory host1 = deployer.hosts.get("TestHost1");
        HostInventory host2 = deployer.hosts.get("TestHost2");

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        UserInventory user = identityCreator.createUser("user", "password");
        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APICreateVmInstanceMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user", "allow");

        SessionInventory session = identityCreator.userLogin(user.getName(), "password");
        VmCreator vmCreator = new VmCreator(api);
        vmCreator.imageUuid = img.getUuid();
        vmCreator.addL3Network(l3.getUuid());
        vmCreator.instanceOfferingUuid = ioinv.getUuid();
        vmCreator.session = session;
        vmCreator.hostUuid = host1.getUuid();
        VmInstanceInventory vm = vmCreator.create();

        boolean success = false;
        try {
            api.stopVmInstance(vm.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.rebootVmInstance(vm.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.migrateVmInstance(vm.getUuid(), host2.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.destroyVmInstance(vm.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        identityCreator.deletePolicy("allow");

        success = false;
        try {
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);
    }

}
