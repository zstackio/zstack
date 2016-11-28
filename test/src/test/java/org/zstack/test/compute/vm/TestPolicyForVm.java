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
import org.zstack.header.query.QueryCondition;
import org.zstack.header.vm.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.ArrayList;

/**
 * 1. create a user
 * 2. assign creating/stopping/rebooting/destroying/migrating permission of allow to the user
 * <p>
 * confirm the user can create/start/stop/reboot/destroy/migrate the vm
 * <p>
 * 3. assign creating/stopping/rebooting/destroying/migrating permission of deny to the user
 * <p>
 * confirm the user can not create/start/stop/reboot/destroy/migrate the vm
 * <p>
 * 4. assign .* permission to the user
 * <p>
 * confirm the user can create/start/stop/reboot/destroy/migrate the vm
 * <p>
 * 5. create a group
 * 6. add the user to the group
 * 7. assign creating/stopping/rebooting/destroying/migrating permission of allow to the group
 * <p>
 * confirm the user can create/start/stop/reboot/destroy/migrate the vm
 * <p>
 * 8. assign creating/stopping/rebooting/destroying/migrating permission of deny to the group
 * <p>
 * confirm the user can not create/start/stop/reboot/destroy/migrate the vm
 * <p>
 * confirm the user can query vms without setting policies
 */
public class TestPolicyForVm {
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
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIDestroyVmInstanceMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIRebootVmInstanceMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIStopVmInstanceMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIStartVmInstanceMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIMigrateVmMsg.class.getSimpleName()));
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

        vm = api.stopVmInstance(vm.getUuid(), session);
        vm = api.startVmInstance(vm.getUuid(), session);
        vm = api.rebootVmInstance(vm.getUuid(), session);
        String targetHostUuid = vm.getHostUuid().equals(host1.getUuid()) ? host2.getUuid() : host1.getUuid();
        api.migrateVmInstance(vm.getUuid(), targetHostUuid, session);
        api.destroyVmInstance(vm.getUuid(), session);

        identityCreator.detachPolicyFromUser("user", "allow");

        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIDestroyVmInstanceMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIRebootVmInstanceMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIStopVmInstanceMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIStartVmInstanceMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APIMigrateVmMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);
        identityCreator.attachPolicyToUser("user", "deny");

        s = new Statement();
        s.setName("allowcreate");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APICreateVmInstanceMsg.class.getSimpleName()));
        identityCreator.createPolicy("allowcreate", s);
        identityCreator.attachPolicyToUser("user", "allowcreate");

        vm = vmCreator.create();

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

        identityCreator.detachPolicyFromUser("user", "allowcreate");

        s = new Statement();
        s.setName("denycreate");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", VmInstanceConstant.ACTION_CATEGORY, APICreateVmInstanceMsg.class.getSimpleName()));
        identityCreator.createPolicy("denycreate", s);
        identityCreator.attachPolicyToUser("user", "denycreate");

        success = false;
        try {
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        identityCreator.detachPolicyFromUser("user", "denycreate");
        identityCreator.detachPolicyFromUser("user", "deny");

        s = new Statement();
        s.setName("allowall");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:.*", VmInstanceConstant.ACTION_CATEGORY));
        identityCreator.createPolicy("allowall", s);
        identityCreator.attachPolicyToUser("user", "allowall");

        vm = vmCreator.create();
        api.stopVmInstance(vm.getUuid(), session);
        api.startVmInstance(vm.getUuid(), session);
        vm = api.rebootVmInstance(vm.getUuid(), session);
        targetHostUuid = vm.getHostUuid().equals(host1.getUuid()) ? host2.getUuid() : host1.getUuid();
        api.migrateVmInstance(vm.getUuid(), targetHostUuid, session);
        api.destroyVmInstance(vm.getUuid(), session);

        // User1 and Group
        identityCreator.createUser("user1", "password");
        identityCreator.createGroup("group");
        identityCreator.attachPolicyToGroup("group", "allow");
        identityCreator.addUserToGroup("user1", "group");

        session = identityCreator.userLogin("user1", "password");
        vmCreator.session = session;
        vm = vmCreator.create();
        api.stopVmInstance(vm.getUuid(), session);
        api.startVmInstance(vm.getUuid(), session);
        vm = api.rebootVmInstance(vm.getUuid(), session);
        targetHostUuid = vm.getHostUuid().equals(host1.getUuid()) ? host2.getUuid() : host1.getUuid();
        api.migrateVmInstance(vm.getUuid(), targetHostUuid, session);
        api.destroyVmInstance(vm.getUuid(), session);

        vm = vmCreator.create();
        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");
        identityCreator.attachPolicyToGroup("group", "denycreate");

        success = false;
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

        success = false;
        try {
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryVmInstanceMsg qmsg = new APIQueryVmInstanceMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qmsg, APIQueryVmInstanceReply.class, session);

        vmCreator.session = identityCreator.accountLogin("test", "password");
        vm = vmCreator.create();

        // operate the vm using another account
        IdentityCreator identityCreator1 = new IdentityCreator(api);
        identityCreator1.createAccount("test2", "password");
        session = identityCreator1.accountLogin("test2", "password");

        success = false;
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

    }
}
