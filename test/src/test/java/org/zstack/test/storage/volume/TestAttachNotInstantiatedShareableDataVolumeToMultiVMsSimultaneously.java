package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APIAttachDataVolumeToVmMsg;
import org.zstack.header.volume.APICreateDataVolumeEvent;
import org.zstack.header.volume.APICreateDataVolumeMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;


public class TestAttachNotInstantiatedShareableDataVolumeToMultiVMsSimultaneously {
    CLogger logger = Utils.getLogger(TestAttachNotInstantiatedShareableDataVolumeToMultiVMsSimultaneously.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    CephPrimaryStorageSimulatorConfig cephConfig;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestAttachShareableDataVolume.xml");
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        cephConfig = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        cephConfig.synchronizedCreateEmptyVolume = true;

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        DiskOfferingInventory diskOfferingInventory = deployer.diskOfferings.get("DiskOffering");
        PrimaryStorageInventory ps1 = deployer.primaryStorages.get("ceph-pri");
        ClusterInventory cluster1 = deployer.clusters.get("Cluster1");

        ApiSender sender = new ApiSender();
        sender.setTimeout(1200);

        String tag = VolumeSystemTags.SHAREABLE.getTagFormat();
        String tag2 = KVMSystemTags.VOLUME_VIRTIO_SCSI.getTagFormat();

        // create NotInstantiated shareable volume
        VolumeInventory vol1;
        APICreateDataVolumeMsg msg = new APICreateDataVolumeMsg();
        msg.setSession(session);
        msg.setName("shareable volume");
        msg.setDiskOfferingUuid(diskOfferingInventory.getUuid());
        msg.setSystemTags(Arrays.asList(tag, tag2));
        APICreateDataVolumeEvent e = sender.send(msg, APICreateDataVolumeEvent.class);
        vol1 = e.getInventory();

        APIAttachDataVolumeToVmMsg msg1 = new APIAttachDataVolumeToVmMsg();
        msg1.setSession(session);
        msg1.setVmUuid(vm.getUuid());
        msg1.setVolumeUuid(vol1.getUuid());
        msg1.setServiceId(ApiMediatorConstant.SERVICE_ID);

        APIAttachDataVolumeToVmMsg msg2 = new APIAttachDataVolumeToVmMsg();
        msg2.setSession(session);
        msg2.setVmInstanceUuid(vm2.getUuid());
        msg2.setVolumeUuid(vol1.getUuid());
        msg2.setServiceId(ApiMediatorConstant.SERVICE_ID);

        List<MessageReply> replies = bus.call(Arrays.asList(msg1, msg2));
        for (MessageReply reply : replies) {
            Assert.assertTrue(reply.isSuccess());
        }
    }
}
