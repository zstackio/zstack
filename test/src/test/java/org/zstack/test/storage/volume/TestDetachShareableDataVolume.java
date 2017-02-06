package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APICreateDataVolumeEvent;
import org.zstack.header.volume.APICreateDataVolumeMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMGlobalConfig;
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


public class TestDetachShareableDataVolume {
    CLogger logger = Utils.getLogger(TestDetachShareableDataVolume.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    String mode = "writethrough";

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

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);

        KVMGlobalConfig.LIBVIRT_CACHE_MODE.updateValue(mode);

        deployer.build();
        api = deployer.getApi();
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        KVMAgentCommands.StartVmCmd cmd = config.startVmCmd;
        for (KVMAgentCommands.VolumeTO to : cmd.getDataVolumes()) {
            Assert.assertEquals(mode, to.getCacheMode());
        }
        Assert.assertEquals(mode, cmd.getRootVolume().getCacheMode());


        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        DiskOfferingInventory diskOfferingInventory = deployer.diskOfferings.get("DiskOffering");
        PrimaryStorageInventory ps1 = deployer.primaryStorages.get("ceph-pri");
        ClusterInventory cluster1 = deployer.clusters.get("Cluster1");

        ApiSender sender = new ApiSender();
        sender.setTimeout(1200);

        VolumeInventory vol;
        {
            APICreateDataVolumeMsg msg = new APICreateDataVolumeMsg();
            msg.setSession(session);
            msg.setPrimaryStorageUuid(ps1.getUuid());
            msg.setName("shareable volume");
            msg.setDiskOfferingUuid(diskOfferingInventory.getUuid());
            String tag = VolumeSystemTags.SHAREABLE.getTagFormat();
            String tag2 = KVMSystemTags.VOLUME_VIRTIO_SCSI.getTagFormat();
            msg.setSystemTags(Arrays.asList(tag, tag2));
            APICreateDataVolumeEvent e = sender.send(msg, APICreateDataVolumeEvent.class);
            vol = e.getInventory();

            Assert.assertTrue(vol.isShareable());
        }

        {
            APIQueryShareableVolumeVmInstanceRefMsg msg = new APIQueryShareableVolumeVmInstanceRefMsg();
            msg.setSession(session);
            msg.addQueryCondition(ShareableVolumeVmInstanceRefVO_.volumeUuid.getName(), QueryOp.EQ, vol.getUuid());
            Assert.assertTrue(0 == api.queryCount(msg, session));
        }

        api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        {
            SimpleQuery<ShareableVolumeVmInstanceRefVO> q = dbf.createQuery(ShareableVolumeVmInstanceRefVO.class);
            Assert.assertTrue(q.count() == 1);

            Assert.assertEquals(1, config.attachDataVolumeCmds.size());
            KVMAgentCommands.AttachDataVolumeCmd acmd = config.attachDataVolumeCmds.get(0);
        }

        api.attachVolumeToVm(vm2.getUuid(), vol.getUuid());
        {
            SimpleQuery<ShareableVolumeVmInstanceRefVO> q = dbf.createQuery(ShareableVolumeVmInstanceRefVO.class);
            Assert.assertTrue(q.count() == 2);

            Assert.assertEquals(2, config.attachDataVolumeCmds.size());
            Assert.assertTrue(config.attachDataVolumeCmds.get(0).getVolume().getWwn().equals(
                    config.attachDataVolumeCmds.get(1).getVolume().getWwn()
            ));
            logger.debug("wwn:" + config.attachDataVolumeCmds.get(0).getVolume().getWwn());
        }

        api.detachVolumeFromVmEx(vol.getUuid(), vm.getUuid(), null);
        {
            SimpleQuery<ShareableVolumeVmInstanceRefVO> q = dbf.createQuery(ShareableVolumeVmInstanceRefVO.class);
            Assert.assertTrue(q.count() == 1);

            Assert.assertEquals(1, config.detachDataVolumeCmds.size());
            Assert.assertTrue(config.detachDataVolumeCmds.get(0).getVolume().getDeviceId() == 2);
        }

        api.detachVolumeFromVmEx(vol.getUuid(), vm2.getUuid(), null);
        {
            SimpleQuery<ShareableVolumeVmInstanceRefVO> q = dbf.createQuery(ShareableVolumeVmInstanceRefVO.class);
            Assert.assertTrue(q.count() == 0);

            Assert.assertEquals(2, config.detachDataVolumeCmds.size());
            Assert.assertTrue(config.detachDataVolumeCmds.get(1).getVolume().getDeviceId() == 2);
        }

    }
}
