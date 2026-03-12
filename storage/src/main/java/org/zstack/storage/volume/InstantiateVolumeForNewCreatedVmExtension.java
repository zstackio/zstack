package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
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
import org.zstack.header.storage.primary.InstantiateRootVolumeForRecoveryMsg;
import org.zstack.header.storage.primary.InstantiateRootVolumeForRecoveryReply;
import org.zstack.header.vm.BeforeGetNextVolumeDeviceIdExtensionPoint;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmAttachVolumeExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.CreateDataVolumeFromVolumeTemplateMsg;
import org.zstack.header.volume.CreateDataVolumeFromVolumeTemplateReply;
import org.zstack.header.volume.InstantiateRootVolumeMsg;
import org.zstack.header.volume.InstantiateVolumeMsg;
import org.zstack.header.volume.InstantiateVolumeReply;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.identity.AccountManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
    }

    private static Integer parseDeviceId(String url) {
        if (url == null) {
            return null;
        }

        try {
            URI uri = new URI(url);
            MultiValueMap<String, String> m = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams();
            List<String> vals = m.getOrDefault("deviceId", Collections.emptyList());
            return vals.isEmpty() ? null : Integer.valueOf(vals.get(0));
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    protected void doInstantiate(final Iterator<NeedReplyMessage> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        NeedReplyMessage msg = it.next();
        bus.send(msg, new CloudBusCallBack(completion) {
            private int allocateDeviceId(String volumeUuid, String imageUuid) {
                Integer deviceId = new SQLBatchWithReturn<Integer>() {
                    @Override
                    protected Integer scripts() {
                        String url = q(ImageVO.class).eq(ImageVO_.uuid, imageUuid).select(ImageVO_.url).findValue();
                        Integer devId = parseDeviceId(url);
                        if (devId == null) {
                            return null;
                        }

                        boolean allocated = q(VolumeVO.class)
                                .eq(VolumeVO_.vmInstanceUuid, spec.getVmInventory().getUuid())
                                .eq(VolumeVO_.deviceId, devId)
                                .notEq(VolumeVO_.uuid, volumeUuid)
                                .isExists();
                        return allocated ? null : devId;
                    }
                }.execute();

                return deviceId != null ? deviceId : getNextDeviceId();
            }

            private int getNextDeviceId(String volumeUuid, String imageUuid) {
                return imageUuid == null ? getNextDeviceId() : allocateDeviceId(volumeUuid, imageUuid);
            }

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

                BitSet full = new BitSet(devIds.size() + 1);
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
                String imageUuid = null;
                if (reply instanceof InstantiateVolumeReply) {
                    VolumeInventory inv = ((InstantiateVolumeReply) reply).getVolume();
                    volumeUuid = inv.getUuid();
                    imageUuid = inv.getRootImageUuid();
                } else if (reply instanceof InstantiateRootVolumeForRecoveryReply) {
                    volumeUuid = ((InstantiateRootVolumeForRecoveryReply) reply).getVolume().getUuid();
                } else if (reply instanceof CreateDataVolumeFromVolumeTemplateReply) {
                    VolumeInventory inv = ((CreateDataVolumeFromVolumeTemplateReply) reply).getInventory();
                    volumeUuid = inv.getUuid();
                    imageUuid = inv.getRootImageUuid();
                } else {
                    throw new CloudRuntimeException("can not be here");
                }

                VolumeVO vo = dbf.findByUuid(volumeUuid, VolumeVO.class);
                if (vo.getType() == VolumeType.Data) {
                    vo.setVmInstanceUuid(vo.isShareable() ? null : spec.getVmInventory().getUuid());
                    vo.setDeviceId(getNextDeviceId(volumeUuid, imageUuid));
                    vo.setActualSize(vo.getActualSize() == null ? 0L : vo.getActualSize());
                } else if (spec.getImageSpec().getInventory() != null) {
                    vo.setActualSize(vo.getActualSize() == null ?
                            spec.getImageSpec().getInventory().getActualSize() : vo.getActualSize());
                }

                vo = dbf.updateAndRefresh(vo);
                List<VmAttachVolumeExtensionPoint> exts = pluginRgty.getExtensionList(VmAttachVolumeExtensionPoint.class);
                for (VmAttachVolumeExtensionPoint ext : exts) {
                    ext.afterInstantiateVolumeForNewCreatedVm(spec.getVmInventory(), VolumeInventory.valueOf(vo));
                }

                vo = dbf.reload(vo);
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
        Map<String, List<String>> volumeTagsMap = Optional.ofNullable(spec.getVolumeSpecs())
                .orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        VmInstanceSpec.VolumeSpec::getAssociatedVolumeUuid,
                        vspec -> vspec.getTags() != null ? vspec.getTags() : Collections.emptyList()
                ));

        for (VolumeInventory volume : spec.getDestDataVolumes()) {
            InstantiateVolumeMsg dmsg = fillMsg(new InstantiateVolumeMsg(), volume, spec);
            List<String> tags = volumeTagsMap.get(volume.getUuid());
            if (!CollectionUtils.isEmpty(tags)) {
                dmsg.setSystemTags(tags);
            }
            msgs.add(dmsg);
        }

        for (VolumeInventory volume : spec.getDestCacheVolumes()) {
            msgs.add(fillMsg(new InstantiateVolumeMsg(), volume, spec));
        }

        ImageSpec image = spec.getImageSpec();
        boolean recovering = false;
        try {
            recovering = image.getSelectedBackupStorage().getInstallPath().startsWith("nbd://") || ImageMediaType.VolumeRecoveringTemplate.toString().equals(image.getInventory().getMediaType());
        } catch (NullPointerException ignored) {
        }

        if (recovering) {
            InstantiateVolumeMsg cmsg = fillMsg(new InstantiateRootVolumeForRecoveryMsg(), spec.getDestRootVolume(), spec);
            ((InstantiateRootVolumeForRecoveryMsg) cmsg).setSelectedBackupStorage(image.getSelectedBackupStorage());
            msgs.add(cmsg);
        } else if (image.getInventory() != null && ImageMediaType.RootVolumeTemplate.toString().equals(image.getInventory().getMediaType())) {
            InstantiateVolumeMsg rmsg = fillMsg(new InstantiateRootVolumeMsg(), spec.getDestRootVolume(), spec);
            ((InstantiateRootVolumeMsg) rmsg).setTemplateSpec(image);
            rmsg.setSystemTags(spec.getRootVolumeSystemTags());
            msgs.add(rmsg);
        } else {
            InstantiateVolumeMsg rmsg = fillMsg(new InstantiateVolumeMsg(), spec.getDestRootVolume(), spec);
            rmsg.setSystemTags(spec.getRootVolumeSystemTags());
            msgs.add(rmsg);
        }

        if (spec.getDataVolumeTemplateUuids() != null) {
            String accountUuid = acntMgr.getOwnerAccountUuidOfResource(spec.getVmInventory().getUuid());
            if (accountUuid == null) {
                throw new CloudRuntimeException(String.format("accountUuid for vm[uuid:%s] is null", spec.getVmInventory().getUuid()));
            }

            String psUuid = spec.getRequiredPrimaryStorageUuidForDataVolume();
            if (psUuid == null) {
                psUuid = spec.getDestRootVolume().getPrimaryStorageUuid();
            }

            for (String imageUuid : spec.getDataVolumeTemplateUuids()) {
                CreateDataVolumeFromVolumeTemplateMsg cmsg = new CreateDataVolumeFromVolumeTemplateMsg();
                cmsg.setHostUuid(spec.getDestHost().getUuid());
                cmsg.setImageUuid(imageUuid);
                cmsg.setPrimaryStorageUuid(psUuid);

                Tuple t = Q.New(ImageVO.class).eq(ImageVO_.uuid, imageUuid).select(ImageVO_.name, ImageVO_.description).findTuple();

                cmsg.setName("data-volume-" + t.get(0, String.class));
                cmsg.setDescription(t.get(1, String.class));
                cmsg.setAccountUuid(accountUuid);
                cmsg.setSystemTags(spec.getDataVolumeFromTemplateSystemTags().get(imageUuid));
                bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
                msgs.add(cmsg);
            }
        }

        doInstantiate(msgs.iterator(), spec, completion);
    }

    private InstantiateVolumeMsg fillMsg(InstantiateVolumeMsg msg, VolumeInventory volume, VmInstanceSpec spec) {
        msg.setVolumeUuid(volume.getUuid());
        msg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
        msg.setHostUuid(spec.getDestHost().getUuid());
        msg.setPrimaryStorageAllocated(true);
        msg.setSkipIfExisting(spec.isInstantiateResourcesSkipExisting());
        msg.setAllocatedInstallUrl(spec.getAllocatedUrlFromVolumeSpecs(volume.getUuid()));
        msg.setSystemTags(spec.getRootVolumeSystemTags());
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volume.getUuid());
        return msg;
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        /* volumes will be deleted when VmAllocateVolumeFlow rolls back */
        completion.success();
    }
}
