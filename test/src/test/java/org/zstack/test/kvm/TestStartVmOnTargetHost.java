package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.APIStartVmInstanceEvent;
import org.zstack.header.vm.APIStartVmInstanceMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestStartVmOnTargetHost {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestStartVmOnTargetHost.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    class VmStarter {
        String vmUuid;
        String clusterUuid;
        String hostUuid;

        VmInstanceInventory start() throws ApiSenderException {
            APIStartVmInstanceMsg msg = new APIStartVmInstanceMsg();
            msg.setUuid(vmUuid);
            msg.setHostUuid(hostUuid);
            msg.setClusterUuid(clusterUuid);
            msg.setSession(api.getAdminSession());
            ApiSender sender = new ApiSender();
            APIStartVmInstanceEvent evt = sender.send(msg, APIStartVmInstanceEvent.class);
            return evt.getInventory();
        }
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        ClusterInventory cluster1 = deployer.clusters.get("Cluster1");
        HostInventory host1 = deployer.hosts.get("host1");
        ClusterInventory cluster2 = deployer.clusters.get("Cluster2");
        HostInventory host2 = deployer.hosts.get("host2");

        api.stopVmInstance(vm.getUuid());

        VmStarter starter = new VmStarter();
        starter.vmUuid = vm.getUuid();
        starter.clusterUuid = cluster2.getUuid();
        vm = starter.start();
        Assert.assertEquals(cluster2.getUuid(), vm.getClusterUuid());

        api.stopVmInstance(vm.getUuid());
        starter = new VmStarter();
        starter.vmUuid = vm.getUuid();
        starter.clusterUuid = cluster1.getUuid();
        vm = starter.start();
        Assert.assertEquals(cluster1.getUuid(), vm.getClusterUuid());

        api.stopVmInstance(vm.getUuid());
        starter = new VmStarter();
        starter.vmUuid = vm.getUuid();
        starter.hostUuid = host2.getUuid();
        vm = starter.start();
        Assert.assertEquals(host2.getUuid(), vm.getHostUuid());

        api.stopVmInstance(vm.getUuid());
        starter = new VmStarter();
        starter.vmUuid = vm.getUuid();
        starter.hostUuid = host1.getUuid();
        vm = starter.start();
        Assert.assertEquals(host1.getUuid(), vm.getHostUuid());

        // set both cluster uuid and host uuid, host uuid works
        api.stopVmInstance(vm.getUuid());
        starter = new VmStarter();
        starter.vmUuid = vm.getUuid();
        starter.clusterUuid = cluster1.getUuid();
        starter.hostUuid = host2.getUuid();
        vm = starter.start();
        Assert.assertEquals(cluster2.getUuid(), vm.getClusterUuid());
        Assert.assertEquals(host2.getUuid(), vm.getHostUuid());
    }

}
