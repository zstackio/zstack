package org.zstack.test.storage.primary.nfs;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

/**
 * 1. add a nfs primary storage
 * 2. create a KVM cluster
 * 3. attach nfs to the cluster
 * 4. add a KVM host to the cluster
 * <p>
 * confirm the primary storage connected
 */
public class TestNfsPrimaryStorageStatus {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    NfsPrimaryStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/OnlyOneZone.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = deployer.zones.get("Zone1");

        PrimaryStorageInventory nfs = new PrimaryStorageInventory();
        nfs.setName("nfs");
        nfs.setUrl("localhost:/nfs");
        nfs.setZoneUuid(zone.getUuid());
        nfs.setTotalCapacity(SizeUnit.TERABYTE.toByte(1));
        nfs.setAvailableCapacity(SizeUnit.TERABYTE.toByte(1));
        nfs = api.addPrimaryStorageByFullConfig(nfs);

        ClusterInventory cluster = new ClusterInventory();
        cluster.setName("cluster");
        cluster.setZoneUuid(zone.getUuid());
        cluster.setHypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE);
        cluster = api.createClusterByFullConfig(cluster);

        api.attachPrimaryStorage(cluster.getUuid(), nfs.getUuid());
        HostInventory kvm = api.addKvmHost("kvm", "localhost", cluster.getUuid());

        PrimaryStorageVO ps = dbf.findByUuid(nfs.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(PrimaryStorageStatus.Connected, ps.getStatus());
    }
}
