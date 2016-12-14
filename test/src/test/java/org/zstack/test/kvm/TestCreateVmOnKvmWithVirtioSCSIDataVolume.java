package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionDSL.list;

public class TestCreateVmOnKvmWithVirtioSCSIDataVolume {
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
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvmWithVirtioSCSIDataVolume.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        PrimaryStorageInventory nfsPS = deployer.primaryStorages.get("nfs");
        HostInventory nfsHost = deployer.hosts.get("host1");
        ClusterInventory nfsCluster = deployer.clusters.get("Cluster1");

        ImageInventory image = deployer.images.get("TestImage");
        InstanceOfferingInventory instanceOffering = deployer.instanceOfferings.get("TestInstanceOffering");
        L3NetworkInventory l3network1 = deployer.l3Networks.get("TestL3Network1");

        DiskOfferingInventory diskOffering1 = deployer.diskOfferings.get("DataDiskOffering1");
        DiskOfferingInventory diskOffering2 = deployer.diskOfferings.get("DataDiskOffering2");

        // create a new vm with nfs ps specified
        VmInstanceInventory testVm = new VmInstanceInventory();
        testVm.setName("testVm");
        testVm.setImageUuid(image.getUuid());
        testVm.setUuid(null);
        testVm.setZoneUuid(null);
        testVm.setClusterUuid(nfsCluster.getUuid());
        testVm.setDefaultL3NetworkUuid(l3network1.getUuid());
        testVm.setInstanceOfferingUuid(instanceOffering.getUuid());
        //
        logger.debug(KVMSystemTags.DISK_OFFERING_VIRTIO_SCSI.getTagFormat());
        String sysTag1 = KVMSystemTags.DISK_OFFERING_VIRTIO_SCSI.getTagFormat();
        sysTag1 = sysTag1.replace("{" + KVMSystemTags.DISK_OFFERING_VIRTIO_SCSI_TOKEN + "}", diskOffering1.getUuid());
        sysTag1 = sysTag1.replace("{" + KVMSystemTags.DISK_OFFERING_VIRTIO_SCSI_NUM_TOKEN + "}", "2");
        testVm = api.createVmByFullConfigWithSpecifiedPS(
                testVm,
                null,
                list(l3network1.getUuid()),
                list(diskOffering1.getUuid(), diskOffering1.getUuid(), diskOffering2.getUuid()),
                list(sysTag1),
                nfsPS.getUuid(),
                session
        );
        Assert.assertEquals(VmInstanceState.Running.toString(), testVm.getState());
        Assert.assertEquals(nfsHost.getUuid(), testVm.getHostUuid());
        Assert.assertTrue(dbf
                .createQuery(SystemTagVO.class)
                .add(SystemTagVO_.tag, SimpleQuery.Op.EQ, KVMSystemTags.VOLUME_VIRTIO_SCSI.getTagFormat())
                .count() == 2);
    }

}
