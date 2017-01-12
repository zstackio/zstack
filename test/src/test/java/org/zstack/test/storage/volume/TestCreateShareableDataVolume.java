package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APICreateDataVolumeEvent;
import org.zstack.header.volume.APICreateDataVolumeMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.mevoco.APIQueryShareableVolumeVmInstanceRefMsg;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO_;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;

/*
 two vms, each has one root volume and one normal data volume
 */
public class TestCreateShareableDataVolume {
    CLogger logger = Utils.getLogger(TestCreateShareableDataVolume.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestAttachShareableDataVolume.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

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
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        DiskOfferingInventory diskOfferingInventory = deployer.diskOfferings.get("DiskOffering");
        PrimaryStorageInventory ps1 = deployer.primaryStorages.get("nfs");
        ClusterInventory cluster1 = deployer.clusters.get("Cluster1");

        ApiSender sender = new ApiSender();
        sender.setTimeout(1200);

        String tag = VolumeSystemTags.SHAREABLE.getTagFormat();
        String tag2 = KVMSystemTags.VOLUME_VIRTIO_SCSI.getTagFormat();

        // create shareable volume
        VolumeInventory vol1;
        {
            APICreateDataVolumeMsg msg = new APICreateDataVolumeMsg();
            msg.setSession(session);
            msg.setPrimaryStorageUuid(ps1.getUuid());
            msg.setName("shareable volume");
            msg.setDiskOfferingUuid(diskOfferingInventory.getUuid());
            msg.setSystemTags(Arrays.asList(tag, tag2));
            APICreateDataVolumeEvent e = sender.send(msg, APICreateDataVolumeEvent.class);
            vol1 = e.getInventory();
        }

        VolumeInventory vol2;
        {
            APICreateDataVolumeMsg msg = new APICreateDataVolumeMsg();
            msg.setSession(session);
            msg.setPrimaryStorageUuid(ps1.getUuid());
            msg.setName("shareable volume");
            msg.setDiskOfferingUuid(diskOfferingInventory.getUuid());
            msg.setSystemTags(Arrays.asList(tag, tag2));
            APICreateDataVolumeEvent e = sender.send(msg, APICreateDataVolumeEvent.class);
            vol2 = e.getInventory();
        }

        Assert.assertTrue(vol1.isShareable());
        Assert.assertTrue(vol2.isShareable());

        // test query api
        APIQueryShareableVolumeVmInstanceRefMsg msg = new APIQueryShareableVolumeVmInstanceRefMsg();
        msg.setSession(session);
        msg.addQueryCondition(ShareableVolumeVmInstanceRefVO_.volumeUuid.getName(), QueryOp.EQ, vol1.getUuid());
        Assert.assertTrue(0 == api.queryCount(msg, session));

        // attach two shareable volumes to one vm
        api.attachVolumeToVm(vm.getUuid(), vol1.getUuid());
        api.attachVolumeToVm(vm.getUuid(), vol2.getUuid());
        Assert.assertTrue(Q.New(ShareableVolumeVmInstanceRefVO.class).count() == 2);
        int deviceId1 = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .select(ShareableVolumeVmInstanceRefVO_.deviceId)
                .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, vol1.getUuid())
                .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vm.getUuid())
                .findValue();
        int deviceId2 = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .select(ShareableVolumeVmInstanceRefVO_.deviceId)
                .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, vol2.getUuid())
                .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vm.getUuid())
                .findValue();
        Assert.assertTrue(deviceId1 != deviceId2);
        Assert.assertTrue(config.attachDataVolumeCmds.size() == 2);
        Assert.assertTrue(config.attachDataVolumeCmds.get(0).getVolume().getDeviceId() == 2);
        Assert.assertTrue(config.attachDataVolumeCmds.get(1).getVolume().getDeviceId() == 3);

        // attach one shareable volume to two vms
        api.attachVolumeToVm(vm2.getUuid(), vol2.getUuid());
        Assert.assertTrue(Q.New(ShareableVolumeVmInstanceRefVO.class).count() == 3);
        Assert.assertTrue(config.attachDataVolumeCmds.size() == 3);
        Assert.assertTrue(config.attachDataVolumeCmds.get(2).getVolume().getDeviceId() == 2);

        // detach
        api.detachVolumeFromVmEx(vol1.getUuid(), vm.getUuid(), null);
        Assert.assertTrue(Q.New(ShareableVolumeVmInstanceRefVO.class).count() == 2);
        Assert.assertTrue(config.detachDataVolumeCmds.size() == 1);
        Assert.assertTrue(config.detachDataVolumeCmds.get(0).getVolume().getDeviceId() == 2);

        //
        api.attachVolumeToVm(vm.getUuid(), vol1.getUuid());
        Assert.assertTrue(Q.New(ShareableVolumeVmInstanceRefVO.class).count() == 3);

        thrown.expect(ApiSenderException.class);
        thrown.expectMessage("you need detach all vm for shareable volume manually before delete.");
        api.detachPrimaryStorage(ps1.getUuid(), cluster1.getUuid());
        api.deletePrimaryStorage(ps1.getUuid());
    }
}
