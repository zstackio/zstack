package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStateEvent;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
 * 3 vms on host1
 * host1 enters maintenance mode
 * host1 quit maintenance mode
 * create new vm on host1
 * result: 3 vms successfully migrate to host2
 * new vm created successfully
 */
public class TestKvmMaintenanceMode6 {
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
        deployer = new Deployer("deployerXml/kvm/TestKvmMaintenance.xml", con);
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
    public void test() throws ApiSenderException, InterruptedException {
        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");
        api.maintainHost(host1.getUuid());
        Assert.assertEquals(3, config.migrateVmCmds.size());
        List<VmInstanceInventory> vms = api.listVmInstances(null);
        for (VmInstanceInventory vm : vms) {
            Assert.assertEquals(VmInstanceState.Running.toString(), vm.getState());
            Assert.assertEquals(host2.getUuid(), vm.getHostUuid());
            Assert.assertEquals(host1.getUuid(), vm.getLastHostUuid());
        }
        host1 = api.listHosts(Arrays.asList(host1.getUuid())).get(0);
        Assert.assertEquals(HostState.Maintenance.toString(), host1.getState());
        api.changeHostState(host1.getUuid(), HostStateEvent.enable);
        // wait host to reconnect
        TimeUnit.SECONDS.sleep(5);
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        vm.setUuid(null);
        vm.setZoneUuid(null);
        vm.setClusterUuid(null);
        vm.setHostUuid(host1.getUuid());
        vm = api.createVmByFullConfig(vm, null, CollectionUtils.transformToList(vm.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getL3NetworkUuid();
            }
        }), null);
        Assert.assertEquals(VmInstanceState.Running.toString(), vm.getState());
        Assert.assertEquals(host1.getUuid(), vm.getHostUuid());
    }
}
