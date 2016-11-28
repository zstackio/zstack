package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashSet;
import java.util.Set;

/**
 * 1. detach all nic
 * <p>
 * confirm the default L3 is null
 * <p>
 * 2. attach a nic
 * <p>
 * confirm the default L3 is set
 * <p>
 * 3. attach more nics
 * 4. update the default L3
 * <p>
 * confirm the default L3 is updated
 * <p>
 * 5. detach a nic
 * 6. attach the nic back
 * <p>
 * confirm no duplicate deviceId
 */
public class TestDetachNicOnKvm7 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network4");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        for (VmNicInventory nic : vm.getVmNics()) {
            vm = api.detachNic(nic.getUuid());
        }

        Assert.assertNull(vm.getDefaultL3NetworkUuid());

        vm = api.attachNic(vm.getUuid(), l3.getUuid());
        Assert.assertEquals(1, vm.getVmNics().size());
        Assert.assertNotNull(vm.getDefaultL3NetworkUuid());


        L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network3");
        vm = api.attachNic(vm.getUuid(), l31.getUuid());
        Assert.assertEquals(l3.getUuid(), vm.getDefaultL3NetworkUuid());

        VmInstanceInventory update = new VmInstanceInventory();
        update.setUuid(vm.getUuid());
        update.setDefaultL3NetworkUuid(l31.getUuid());
        vm = api.updateVm(update);
        Assert.assertEquals(l31.getUuid(), vm.getDefaultL3NetworkUuid());

        VmNicInventory tnic = vm.getVmNics().get(0);
        api.detachNic(tnic.getUuid());
        vm = api.attachNic(vm.getUuid(), tnic.getL3NetworkUuid());
        vm = api.attachNic(vm.getUuid(), deployer.l3Networks.get("TestL3Network1").getUuid());
        vm = api.attachNic(vm.getUuid(), deployer.l3Networks.get("TestL3Network2").getUuid());
        Set<Integer> deviceIds = new HashSet<Integer>();
        for (VmNicInventory nic : vm.getVmNics()) {
            deviceIds.add(nic.getDeviceId());
        }
        Assert.assertEquals(vm.getVmNics().size(), deviceIds.size());
    }
}
