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
import org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsReply;
import org.zstack.header.vm.APIStartVmInstanceEvent;
import org.zstack.header.vm.APIStartVmInstanceMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestStartVmOnTargetHost1 {
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

        boolean s = false;
        try {
            // vm is running, failure
            api.getVmStartingCandidateHosts(vm.getUuid(), null);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        api.stopVmInstance(vm.getUuid());
        APIGetVmStartingCandidateClustersHostsReply reply = api.getVmStartingCandidateHosts(vm.getUuid(), null);
        Assert.assertEquals(2, reply.getClusterInventories().size());
        Assert.assertEquals(2, reply.getHostInventories().size());
    }

}
