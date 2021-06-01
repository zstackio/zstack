package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Od;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DownloadVolumeTemplateToPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;

public class InstantiateVolumeForNewCreatedVmExtension implements PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(InstantiateVolumeForNewCreatedVmExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException{
    }

    protected void doInstantiate(final Iterator<InstantiateVolumeMsg> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        InstantiateVolumeMsg msg  = it.next();
        bus.send(msg, new CloudBusCallBack(completion) {
            private int getNextDeviceId() {
                SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
                q.select(VolumeVO_.deviceId);
                q.add(VolumeVO_.vmInstanceUuid, Op.EQ, spec.getVmInventory().getUuid());
                q.add(VolumeVO_.deviceId, Op.NOT_NULL);
                q.orderBy(VolumeVO_.deviceId, Od.ASC);
                List<Integer> devIds = q.listValue();

                for (BeforeGetNextVolumeDeviceIdExtensionPoint e : pluginRgty.getExtensionList(BeforeGetNextVolumeDeviceIdExtensionPoint.class)) {
                    e.beforeGetNextVolumeDeviceId(spec.getVmInventory().getUuid(), devIds);
                }

                BitSet full = new BitSet(devIds.size()+1);
                for (Integer id : devIds) {
                    full.set(id);
                }
                return full.nextClearBit(0);
            }

            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    InstantiateVolumeReply r = reply.castReply();
                    VolumeVO vo = dbf.findByUuid(r.getVolume().getUuid(), VolumeVO.class);
                    if (vo.getType() == VolumeType.Data) {
                        vo.setDeviceId(getNextDeviceId());
                        vo.setActualSize(0L);
                    } else if (spec.getImageSpec().getInventory() != null) {
                        vo.setActualSize(spec.getImageSpec().getInventory().getActualSize());
                    }

                    vo = dbf.updateAndRefresh(vo);
                    VolumeInventory vinv = VolumeInventory.valueOf(vo);
                    if (spec.getDestRootVolume().getUuid().equals(vinv.getUuid())) {
                        spec.setDestRootVolume(vinv);
                    } else if (VolumeType.Data.toString().equals(vinv.getType())) {
                        // Delete the original volumeInventory, and then re-add latest volumeInventory, the latest volumeInventory contains more attributes
                        spec.getDestDataVolumes().removeIf(volumeInventory -> msg.getVolumeUuid().equals(volumeInventory.getUuid()));
                        spec.getDestDataVolumes().add(vinv);
                    } else if (VolumeType.Cache.toString().equals(vinv.getType())) {
                        spec.getDestCacheVolumes().removeIf(volumeInventory -> msg.getVolumeUuid().equals(volumeInventory.getUuid()));
                        spec.getDestCacheVolumes().add(vinv);
                    }

                    logger.debug(String.format("spec.getDestRootVolume is: %s", spec.getDestRootVolume().getInstallPath()));
                    logger.debug(String.format("successfully instantiated volume%s", JSONObjectUtil.toJsonString(vinv)));

                    doInstantiate(it, spec, completion);
                } else {
                    completion.fail(reply.getError());
                }

            }
        });
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (VmInstanceConstant.VmOperation.NewCreate != spec.getCurrentVmOperation()) {
            completion.success();
            return;
        }

        List<InstantiateVolumeMsg> msgs = new ArrayList<>();
        for (VolumeInventory volume : spec.getDestDataVolumes()) {
            InstantiateVolumeMsg msg = new InstantiateVolumeMsg();
            msg.setVolumeUuid(volume.getUuid());
            msg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
            msg.setHostUuid(spec.getDestHost().getUuid());
            msg.setPrimaryStorageAllocated(true);
            msg.setSkipIfExisting(spec.isInstantiateResourcesSkipExisting());
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volume.getUuid());
            msgs.add(msg);
        }

        for (VolumeInventory volume : spec.getDestCacheVolumes()) {
            InstantiateVolumeMsg msg = new InstantiateVolumeMsg();
            msg.setVolumeUuid(volume.getUuid());
            msg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
            msg.setHostUuid(spec.getDestHost().getUuid());
            msg.setPrimaryStorageAllocated(true);
            msg.setSkipIfExisting(spec.isInstantiateResourcesSkipExisting());
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volume.getUuid());
            msgs.add(msg);
        }

        ImageSpec image = spec.getImageSpec();
        InstantiateVolumeMsg rmsg;
        if (image.getInventory() != null && ImageMediaType.RootVolumeTemplate.toString().equals(image.getInventory().getMediaType())) {
            rmsg = new InstantiateRootVolumeMsg();
            ((InstantiateRootVolumeMsg)rmsg).setTemplateSpec(image);
        } else {
            rmsg = new InstantiateVolumeMsg();
        }
        rmsg.setPrimaryStorageUuid(spec.getDestRootVolume().getPrimaryStorageUuid());
        rmsg.setHostUuid(spec.getDestHost().getUuid());
        rmsg.setVolumeUuid(spec.getDestRootVolume().getUuid());
        rmsg.setPrimaryStorageAllocated(true);
        rmsg.setSkipIfExisting(spec.isInstantiateResourcesSkipExisting());
        bus.makeTargetServiceIdByResourceUuid(rmsg, VolumeConstant.SERVICE_ID, spec.getDestRootVolume().getUuid());
        msgs.add(rmsg);

        if (ImageMediaType.Kernel.toString().equals(image.getInventory().getMediaType())) {
            DownloadVolumeTemplateToPrimaryStorageMsg dmsg = new DownloadVolumeTemplateToPrimaryStorageMsg();
            dmsg.setTemplateSpec(image);
            dmsg.setHostUuid(spec.getDestHost().getUuid());
            dmsg.setPrimaryStorageUuid(spec.getDestRootVolume().getPrimaryStorageUuid());
            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, dmsg.getPrimaryStorageUuid());
            bus.send(dmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                }
            });
        }

        if (msgs.isEmpty()) {
            completion.success();
            return;
        }
        doInstantiate(msgs.iterator(), spec, completion);
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        /* volumes will be deleted when VmAllocateVolumeFlow rolls back */
        completion.success();
    }
}
