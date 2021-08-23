package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Od;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;

public class InstantiateVolumeForNewCreatedVmExtension implements PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(InstantiateVolumeForNewCreatedVmExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected AccountManager acntMgr;

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException{
    }

    protected void doInstantiate(final Iterator<NeedReplyMessage> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        NeedReplyMessage msg  = it.next();
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
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                String volumeUuid;
                if (reply instanceof InstantiateVolumeReply) {
                    volumeUuid = ((InstantiateVolumeReply) reply).getVolume().getUuid();
                } else if (reply instanceof CreateDataVolumeFromVolumeTemplateReply){
                    volumeUuid = ((CreateDataVolumeFromVolumeTemplateReply) reply).getInventory().getUuid();
                } else {
                    throw new CloudRuntimeException("can not be here");
                }

                VolumeVO vo = dbf.findByUuid(volumeUuid, VolumeVO.class);
                if (vo.getType() == VolumeType.Data) {
                    vo.setVmInstanceUuid(spec.getVmInventory().getUuid());
                    vo.setDeviceId(getNextDeviceId());
                    vo.setActualSize(vo.getActualSize() == null ? 0L : vo.getActualSize());
                } else if (spec.getImageSpec().getInventory() != null) {
                    vo.setActualSize(spec.getImageSpec().getInventory().getActualSize());
                }

                vo = dbf.updateAndRefresh(vo);
                VolumeInventory vinv = VolumeInventory.valueOf(vo);
                if (spec.getDestRootVolume().getUuid().equals(vinv.getUuid())) {
                    spec.setDestRootVolume(vinv);
                } else if (VolumeType.Data.toString().equals(vinv.getType())) {
                    // Delete the original volumeInventory, and then re-add latest volumeInventory, the latest volumeInventory contains more attributes
                    spec.getDestDataVolumes().removeIf(volumeInventory -> volumeUuid.equals(volumeInventory.getUuid()));
                    spec.getDestDataVolumes().add(vinv);
                } else if (VolumeType.Cache.toString().equals(vinv.getType())) {
                    spec.getDestCacheVolumes().removeIf(volumeInventory -> volumeUuid.equals(volumeInventory.getUuid()));
                    spec.getDestCacheVolumes().add(vinv);
                }

                logger.debug(String.format("spec.getDestRootVolume is: %s", spec.getDestRootVolume().getInstallPath()));
                logger.debug(String.format("successfully instantiated volume%s", JSONObjectUtil.toJsonString(vinv)));

                doInstantiate(it, spec, completion);
            }
        });
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (VmInstanceConstant.VmOperation.NewCreate != spec.getCurrentVmOperation()) {
            completion.success();
            return;
        }

        List<NeedReplyMessage> msgs = new ArrayList<>();
        for (VolumeInventory volume : spec.getDestDataVolumes()) {
            msgs.add(fillMsg(new InstantiateVolumeMsg(), volume, spec));
        }

        for (VolumeInventory volume : spec.getDestCacheVolumes()) {
            msgs.add(fillMsg(new InstantiateVolumeMsg(), volume, spec));
        }

        ImageSpec image = spec.getImageSpec();
        if (image.getInventory() != null && ImageMediaType.RootVolumeTemplate.toString().equals(image.getInventory().getMediaType())) {
            InstantiateVolumeMsg rmsg = fillMsg(new InstantiateRootVolumeMsg(), spec.getDestRootVolume(), spec) ;
            ((InstantiateRootVolumeMsg)rmsg).setTemplateSpec(image);
            msgs.add(rmsg);
        } else {
            msgs.add(fillMsg(new InstantiateVolumeMsg(), spec.getDestRootVolume(), spec));
        }

        if (spec.getDataVolumeTemplateUuids() != null) {
            String accountUuid = acntMgr.getOwnerAccountUuidOfResource(spec.getVmInventory().getUuid());
            if (accountUuid == null) {
                throw new CloudRuntimeException(String.format("accountUuid for vm[uuid:%s] is null", spec.getVmInventory().getUuid()));
            }

            for (String imageUuid : spec.getDataVolumeTemplateUuids()) {
                CreateDataVolumeFromVolumeTemplateMsg cmsg = new CreateDataVolumeFromVolumeTemplateMsg();
                cmsg.setHostUuid(spec.getDestHost().getUuid());
                cmsg.setImageUuid(imageUuid);
                if (spec.getRequiredPrimaryStorageUuidForDataVolume() != null) {
                    cmsg.setPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForDataVolume());
                } else {
                    cmsg.setPrimaryStorageUuid(spec.getDestRootVolume().getPrimaryStorageUuid());
                }

                Tuple t = Q.New(ImageVO.class).eq(ImageVO_.uuid, imageUuid).select(ImageVO_.name, ImageVO_.description).findTuple();

                cmsg.setName("data-volume-" + t.get(0, String.class));
                cmsg.setDescription(t.get(1, String.class));
                cmsg.setAccountUuid(accountUuid);
                cmsg.setSystemTags(spec.getDataVolumeFromTemplateSystemTags().get(imageUuid));
                bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
                msgs.add(cmsg);
            }
        }

        if (msgs.isEmpty()) {
            completion.success();
            return;
        }

        doInstantiate(msgs.iterator(), spec, completion);
    }

    private InstantiateVolumeMsg fillMsg(InstantiateVolumeMsg msg, VolumeInventory volume, VmInstanceSpec spec) {
        msg.setVolumeUuid(volume.getUuid());
        msg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
        msg.setHostUuid(spec.getDestHost().getUuid());
        //new
        msg.setAllocatedInstallUrl(spec.getDestHost().getUuid());
        msg.setPrimaryStorageAllocated(true);
        msg.setSkipIfExisting(spec.isInstantiateResourcesSkipExisting());
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volume.getUuid());
        return msg;
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        /* volumes will be deleted when VmAllocateVolumeFlow rolls back */
        completion.success();
    }
}
