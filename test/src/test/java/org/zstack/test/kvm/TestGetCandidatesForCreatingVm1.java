package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmMsg;
import org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static java.util.Arrays.asList;

public class TestGetCandidatesForCreatingVm1 {
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
        deployer = new Deployer("deployerXml/kvm/TestGetCandidatesForCreatingVm1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        ImageInventory img = deployer.images.get("TestImage");
        InstanceOfferingInventory inso = deployer.instanceOfferings.get("TestInstanceOffering");
        L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network1");

        APIGetCandidateZonesClustersHostsForCreatingVmMsg msg = new APIGetCandidateZonesClustersHostsForCreatingVmMsg();
        msg.setImageUuid(img.getUuid());
        msg.setL3NetworkUuids(asList(l31.getUuid()));
        msg.setInstanceOfferingUuid(inso.getUuid());
        msg.setSession(api.getAdminSession());
        ApiSender sender = api.getApiSender();
        APIGetCandidateZonesClustersHostsForCreatingVmReply reply = sender.call(msg, APIGetCandidateZonesClustersHostsForCreatingVmReply.class);

        Assert.assertEquals(1, reply.getZones().size());
        ZoneInventory zone1 = deployer.zones.get("Zone1");
        Assert.assertEquals(zone1.getUuid(), reply.getZones().get(0).getUuid());

        Assert.assertEquals(1, reply.getClusters().size());
        ClusterInventory cluster1 = deployer.clusters.get("Cluster2");
        Assert.assertEquals(cluster1.getUuid(), reply.getClusters().get(0).getUuid());

        Assert.assertEquals(1, reply.getHosts().size());
        HostInventory host1 = deployer.hosts.get("host2");
        Assert.assertEquals(host1.getUuid(), reply.getHosts().get(0).getUuid());
    }
}
