package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.configuration.DiskOfferingSystemTags;
import org.zstack.configuration.OfferingUserConfigUtils;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfig;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageState;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.snapshot.ConsistentType;
import org.zstack.header.storage.snapshot.group.MemorySnapshotValidatorExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class VolumeApiInterceptor implements ApiMessageInterceptor, Component {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    private Map<String, MaxDataVolumeNumberExtensionPoint> maxDataVolumeNumberExtensions = new ConcurrentHashMap<String, MaxDataVolumeNumberExtensionPoint>();
    private static final int DEFAULT_MAX_DATA_VOLUME_NUMBER = 24;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VolumeMessage) {
            VolumeMessage vmsg = (VolumeMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, vmsg.getVolumeUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIChangeVolumeStateMsg) {
            validate((APIChangeVolumeStateMsg) msg);
        } else if (msg instanceof APIDeleteDataVolumeMsg) {
            validate((APIDeleteDataVolumeMsg) msg);
        } else if (msg instanceof APICreateDataVolumeMsg) {
            validate((APICreateDataVolumeMsg) msg);
        } else if (msg instanceof APIBackupDataVolumeMsg) {
            validate((APIBackupDataVolumeMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToVmMsg) {
            validate((APIAttachDataVolumeToVmMsg) msg);
        } else if (msg instanceof APIDetachDataVolumeFromVmMsg) {
            validate((APIDetachDataVolumeFromVmMsg) msg);
        } else if (msg instanceof APIGetDataVolumeAttachableVmMsg) {
            validate((APIGetDataVolumeAttachableVmMsg) msg);
        } else if (msg instanceof APICreateDataVolumeFromVolumeTemplateMsg) {
            validate((APICreateDataVolumeFromVolumeTemplateMsg) msg);
        } else if (msg instanceof APIRecoverDataVolumeMsg) {
            validate((APIRecoverDataVolumeMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotGroupMsg) {
            validate((APICreateVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotMsg) {
            validate((APICreateVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToHostMsg) {
            validate((APIAttachDataVolumeToHostMsg) msg);
        } else if (msg instanceof APIDetachDataVolumeFromHostMsg) {
            validate((APIDetachDataVolumeFromHostMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APICreateVolumeSnapshotMsg msg) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.status, VolumeVO_.type, VolumeVO_.state);
        q.add(VolumeVO_.uuid, Op.EQ, msg.getVolumeUuid());
        Tuple tuple = q.findTuple();
        VolumeStatus status = (VolumeStatus) tuple.get(0);
        if (status != VolumeStatus.Ready) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is not in status Ready, current is %s, can't create snapshot", msg.getVolumeUuid(), status));
        }

        VolumeType type = (VolumeType) tuple.get(1);
        if (type != VolumeType.Root && type != VolumeType.Data) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s, type:%s], can't create snapshot", msg.getVolumeUuid(), type));
        }

        VolumeState state = (VolumeState) tuple.get(2);
        if (state != VolumeState.Enabled) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is not in state Enabled, " +
                    "current is %s, can't create snapshot", msg.getVolumeUuid(), state));
        }
    }

    private void validate(APICreateVolumeSnapshotGroupMsg msg) {
        VmInstanceVO vmvo = SQL.New("select vm from VmInstanceVO vm, VolumeVO vol" +
                " where vol.uuid = :volUuid" +
                " and vol.type = :volType" +
                " and vm.uuid = vol.vmInstanceUuid", VmInstanceVO.class)
                .param("volType", VolumeType.Root)
                .param("volUuid", msg.getRootVolumeUuid())
                .find();

        if (vmvo == null) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] is not root volume", msg.getRootVolumeUuid()));
        }

        if (msg.isWithMemory() && !(vmvo.getState().equals(VmInstanceState.Running) || (vmvo.getState().equals(VmInstanceState.Paused)))) {
            throw new ApiMessageInterceptionException(argerr("Can not take memory snapshot, vm current state[%s], but expect state are [%s, %s]",
                    vmvo.getState().toString(), VmInstanceState.Running.toString(), VmInstanceState.Paused.toString()));
        }

        for (VolumeVO vol : vmvo.getAllVolumes()) {
            if (vol.getStatus() != VolumeStatus.Ready) {
                throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is not in status Ready, " +
                        "current is %s, can't create snapshot", vol.getUuid(), vol.getStatus()));
            }
        }

        msg.setVmInstance(VmInstanceInventory.valueOf(vmvo));
        if (msg.isWithMemory()) {
            msg.setConsistentType(ConsistentType.Application);
        }

        for (MemorySnapshotValidatorExtensionPoint ext : pluginRgty.getExtensionList(MemorySnapshotValidatorExtensionPoint.class)) {
            if (!msg.isWithMemory()) {
                break;
            }

            ErrorCode errorCode = ext.checkVmWhereMemorySnapshotExistExternalDevices(msg.getVmInstance().getUuid());
            if (errorCode != null) {
                throw new ApiMessageInterceptionException(errorCode);
            }
        }
    }

    private void validate(APIRecoverDataVolumeMsg msg) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.add(VolumeVO_.uuid, Op.EQ, msg.getVolumeUuid());
        q.add(VolumeVO_.status, Op.EQ, VolumeStatus.Deleted);
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(operr("the volume[uuid:%s] is not in status of deleted. This is operation is to recover a deleted data volume",
                    msg.getVolumeUuid()));
        }
    }

    private void exceptionIsVolumeIsDeleted(String volumeUuid) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.add(VolumeVO_.uuid, Op.EQ, volumeUuid);
        q.add(VolumeVO_.status, Op.EQ, VolumeStatus.Deleted);
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("the volume[uuid:%s] is in status of deleted, cannot do the operation", volumeUuid));
        }
    }

    private void validate(APICreateDataVolumeFromVolumeTemplateMsg msg) {
        ImageVO img = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        ImageMediaType type = img.getMediaType();
        if (ImageMediaType.DataVolumeTemplate != type) {
            throw new ApiMessageInterceptionException(argerr("image[uuid:%s] is not %s, it's %s", msg.getImageUuid(), ImageMediaType.DataVolumeTemplate, type));
        }

        if (ImageState.Enabled != img.getState()) {
            throw new ApiMessageInterceptionException(operr("image[uuid:%s] is not Enabled, it's %s", img.getUuid(), img.getState()));
        }

        if (ImageStatus.Ready != img.getStatus()) {
            throw new ApiMessageInterceptionException(operr("image[uuid:%s] is not Ready, it's %s", img.getUuid(), img.getStatus()));
        }
    }

    private void validate(APIGetDataVolumeAttachableVmMsg msg) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.vmInstanceUuid, VolumeVO_.state, VolumeVO_.status, VolumeVO_.type);
        q.add(VolumeVO_.uuid, Op.EQ, msg.getVolumeUuid());
        Tuple t = q.findTuple();

        VolumeType type = t.get(3, VolumeType.class);
        if (type == VolumeType.Root) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] is Root volume, can not be attach to vm", msg.getVolumeUuid()));
        }

        // As per issue #1696, we do not report error if the volume has been attached.
        // Instead, an empty list will be returned later when handling this message.
        VolumeState state = t.get(1, VolumeState.class);
        if (state != VolumeState.Enabled) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] is in state[%s], data volume can only be attached when state is %s", msg.getVolumeUuid(), state, VolumeState.Enabled));
        }

        VolumeStatus status = t.get(2, VolumeStatus.class);
        if (status != VolumeStatus.Ready && status != VolumeStatus.NotInstantiated) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] is in status[%s], data volume can only be attached when status is %s or %S", msg.getVolumeUuid(), status, VolumeStatus.Ready, VolumeStatus.NotInstantiated));
        }
    }

    private void validate(APIDetachDataVolumeFromVmMsg msg) {
        VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (!vol.isShareable() && vol.getVmInstanceUuid() == null) {
            throw new ApiMessageInterceptionException(operr("data volume[uuid:%s] is not attached to any vm, can't detach", msg.getVolumeUuid()));
        }

        if (vol.isShareable() && msg.getVmUuid() == null) {
            throw new ApiMessageInterceptionException(operr("to detach shareable data volume[uuid:%s], vm uuid is needed.", msg.getVolumeUuid()));
        }


        if (vol.getType() != VolumeType.Data) {
            throw new ApiMessageInterceptionException(operr("the volume[uuid:%s, name:%s, type:%s] can't detach it",
                    vol.getUuid(), vol.getName(), vol.getType()));
        }
    }

    private void validate(APIAttachDataVolumeToVmMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VolumeVO vol = q(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
                List<String> volumeClusterUuids = q(PrimaryStorageClusterRefVO.class)
                        .select(PrimaryStorageClusterRefVO_.clusterUuid)
                        .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, vol.getPrimaryStorageUuid())
                        .listValues();

                List<String> vmInstanceClusterUuids = new ArrayList<>();
                if (!q(VmInstanceVO.class)
                        .isNull(VmInstanceVO_.clusterUuid)
                        .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                        .isExists()) {
                    vmInstanceClusterUuids.add(q(VmInstanceVO.class)
                            .select(VmInstanceVO_.clusterUuid)
                            .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                            .findValue());
                }

                if (vmInstanceClusterUuids.isEmpty()) {
                    String vmRootVolumeUuid = q(VmInstanceVO.class).select(VmInstanceVO_.rootVolumeUuid)
                            .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).findValue();

                    String vmPrimaryStorageUuid = q(VolumeVO.class).select(VolumeVO_.primaryStorageUuid)
                            .eq(VolumeVO_.uuid, vmRootVolumeUuid).findValue();

                    vmInstanceClusterUuids = q(PrimaryStorageClusterRefVO.class)
                            .select(PrimaryStorageClusterRefVO_.clusterUuid)
                            .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, vmPrimaryStorageUuid)
                            .listValues();
                }

                vmInstanceClusterUuids.retainAll(volumeClusterUuids);

                // if there is no cluster contains both vm root volume and data volume, the data volume won't be attachable
                if (vmInstanceClusterUuids.isEmpty() && !volumeClusterUuids.isEmpty()) {
                    throw new ApiMessageInterceptionException(operr("Can't attach volume to VM, no qualified cluster"));
                }

                long count = sql("select count(vm.uuid)" +
                        " from VmInstanceVO vm, ImageVO image" +
                        " where vm.uuid = :vmUuid" +
                        " and vm.imageUuid = image.uuid" +
                        " and image.platform = :platformType" +
                        " and vm.state != :vmState")
                        .param("vmUuid", msg.getVmInstanceUuid())
                        .param("vmState", VmInstanceState.Stopped)
                        .param("platformType", ImagePlatform.Other).find();
                if (count > 0) {
                    throw new ApiMessageInterceptionException(operr("the vm[uuid:%s] doesn't support to online attach volume[%s] on the basis of that the image platform type of the vm is other ", msg.getVmInstanceUuid(), msg.getVolumeUuid()));
                }


                if (vol.getType() == VolumeType.Root) {
                    throw new ApiMessageInterceptionException(operr("the volume[uuid:%s, name:%s] is Root Volume, can't attach it",
                            vol.getUuid(), vol.getName()));
                }

                if (vol.getState() == VolumeState.Disabled) {
                    throw new ApiMessageInterceptionException(operr("data volume[uuid:%s] is Disabled, can't attach", vol.getUuid()));
                }

                if (vol.getStatus() == VolumeStatus.Deleted) {
                    throw new ApiMessageInterceptionException(operr("the volume[uuid:%s] is in status of deleted, cannot do the operation", vol.getUuid()));
                }

                if (vol.isAttached() && !vol.isShareable()) {
                    throw new ApiMessageInterceptionException(operr("data volume[uuid:%s] has been attached to some vm, can't attach again",
                            vol.getUuid()));
                }

                if (VolumeStatus.Ready != vol.getStatus() && VolumeStatus.NotInstantiated != vol.getStatus()) {
                    throw new ApiMessageInterceptionException(operr("data volume can only be attached when status is [%s, %s], current is %s",
                            VolumeStatus.Ready, VolumeStatus.NotInstantiated, vol.getStatus()));
                }

                String hvType = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).select(VmInstanceVO_.hypervisorType).findValue();
                if (vol.getFormat() != null) {
                    List<String> hvTypes = VolumeFormat.valueOf(vol.getFormat()).getHypervisorTypesSupportingThisVolumeFormatInString();
                    if (!hvTypes.contains(hvType)) {
                        throw new ApiMessageInterceptionException(operr("data volume[uuid:%s] has format[%s] that can only be attached to hypervisor[%s], but vm[uuid:%s] has hypervisor type[%s]. Can't attach",
                                vol.getUuid(), vol.getFormat(), hvTypes, msg.getVmInstanceUuid(), hvType));
                    }
                }

                MaxDataVolumeNumberExtensionPoint ext = maxDataVolumeNumberExtensions.get(hvType);
                int maxDataVolumeNum = DEFAULT_MAX_DATA_VOLUME_NUMBER;
                if (ext != null) {
                    maxDataVolumeNum = ext.getMaxDataVolumeNumber();
                }

                count = Q.New(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data).eq(VolumeVO_.vmInstanceUuid, msg.getVmInstanceUuid()).count();
                if (count + 1 > maxDataVolumeNum) {
                    throw new ApiMessageInterceptionException(operr("hypervisor[%s] only allows max %s data volumes to be attached to a single vm; there have been %s data volumes attached to vm[uuid:%s]",
                            hvType, maxDataVolumeNum, count, msg.getVmInstanceUuid()));
                }


            }
        }.execute();

    }

    private void validate(APIBackupDataVolumeMsg msg) {
        if (isRootVolume(msg.getUuid())) {
            throw new ApiMessageInterceptionException(operr("it's not allowed to backup root volume, uuid:%s", msg.getUuid()));
        }

        exceptionIsVolumeIsDeleted(msg.getVolumeUuid());
    }

    private void validate(APICreateDataVolumeMsg msg) {
        if (msg.getDiskOfferingUuid() == null) {
            if (msg.getDiskSize() < 0) {
                throw new ApiMessageInterceptionException(argerr("unexpected disk size settings"));
            }
        } else {
            Long diskSize = Q.New(DiskOfferingVO.class).eq(DiskOfferingVO_.uuid, msg.getDiskOfferingUuid()).select(DiskOfferingVO_.diskSize).findValue();
            msg.setDiskSize(diskSize);
        }

        String diskOffering = msg.getDiskOfferingUuid();
        if (diskOffering == null) {
            return;
        }

        if (DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(diskOffering)) {
            DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(diskOffering, DiskOfferingUserConfig.class);
            if (config.getAllocate() == null || config.getAllocate().getPrimaryStorage() == null) {
                return;
            }

            String psUuid = config.getAllocate().getPrimaryStorage().getUuid();
            if (msg.getPrimaryStorageUuid() != null && !msg.getPrimaryStorageUuid().equals(psUuid)) {
                throw new ApiMessageInterceptionException(argerr("primaryStorageUuid conflict, the primary storage specified by the disk offering is %s, and the primary storage specified in the creation parameter is %s",
                        psUuid, msg.getPrimaryStorageUuid()));
            }
            msg.setPrimaryStorageUuid(psUuid);
        }
    }

    private void validate(APIDeleteDataVolumeMsg msg) {
        if (!dbf.isExist(msg.getUuid(), VolumeVO.class)) {
            APIDeleteDataVolumeEvent evt = new APIDeleteDataVolumeEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.type, VolumeVO_.status);
        q.add(VolumeVO_.uuid, Op.EQ, msg.getVolumeUuid());
        Tuple t = q.findTuple();
        VolumeType type = t.get(0, VolumeType.class);
        if (type != VolumeType.Data) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s, type:%s] can't be deleted", msg.getVolumeUuid(), type));
        }

        VolumeStatus status = t.get(1, VolumeStatus.class);
        if (status == VolumeStatus.Deleted) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is already in status of deleted", msg.getVolumeUuid()));
        }

        String hostUuid = Q.New(VolumeHostRefVO.class).select(VolumeHostRefVO_.hostUuid)
                .eq(VolumeHostRefVO_.volumeUuid, msg.getUuid()).findValue();
        if (hostUuid != null) {
            throw new ApiMessageInterceptionException(argerr("can not delete volume[%s], " +
                    "because volume attach to host[%s]", msg.getVolumeUuid(), hostUuid));
        }
    }

    private boolean isRootVolume(String uuid) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.type);
        q.add(VolumeVO_.uuid, Op.EQ, uuid);
        VolumeType type = q.findValue();
        return type == VolumeType.Root;
    }

    private void validate(APIChangeVolumeStateMsg msg) {
        if (isRootVolume(msg.getUuid())) {
            throw new ApiMessageInterceptionException(operr("it's not allowed to change state of root volume, uuid:%s", msg.getUuid()));
        }

        exceptionIsVolumeIsDeleted(msg.getVolumeUuid());

        String hostUuid = Q.New(VolumeHostRefVO.class).select(VolumeHostRefVO_.hostUuid)
                .eq(VolumeHostRefVO_.volumeUuid, msg.getUuid()).findValue();
        if (hostUuid != null) {
            throw new ApiMessageInterceptionException(argerr("can not change volume[%s] state, " +
                    "because volume attach to host[%s]", msg.getVolumeUuid(), hostUuid));
        }
    }

    private void validate(APIAttachDataVolumeToHostMsg msg) {
        String attachVolumeErr = i18n("can not attach volume[%s] to host[%s], ", msg.getVolumeUuid(), msg.getHostUuid());

        HostStatus hostStatus = Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, msg.getHostUuid()).findValue();
        if (hostStatus != HostStatus.Connected) {
            throw new ApiMessageInterceptionException(operr(attachVolumeErr + "because host[status:%s] is not connected", hostStatus));
        }

        if (!msg.getMountPath().startsWith("/")) {
            throw new ApiMessageInterceptionException(argerr("mount path must be absolute path"));
        }

        Tuple hostAndMountPath = Q.New(VolumeHostRefVO.class)
                .eq(VolumeHostRefVO_.volumeUuid, msg.getVolumeUuid())
                .select(VolumeHostRefVO_.hostUuid, VolumeHostRefVO_.mountPath).findTuple();
        if (hostAndMountPath != null) {
            doValidateAttachedVolume(hostAndMountPath, msg, attachVolumeErr);
        } else {
            checkMountPathOnHost(msg, attachVolumeErr);
        }
    }

    private void doValidateAttachedVolume(Tuple hostAndMountPath, APIAttachDataVolumeToHostMsg msg, String attachVolumeErr) {
        String hostUuid = hostAndMountPath.get(0, String.class);
        String mountPath = hostAndMountPath.get(1, String.class);
        if (!hostUuid.equals(msg.getHostUuid())) {
            throw new ApiMessageInterceptionException(operr(attachVolumeErr +
                    "because volume is attaching to host[%s] ", hostUuid));
        }
        if (!mountPath.equals(msg.getMountPath())) {
            throw new ApiMessageInterceptionException(operr(attachVolumeErr +
                    "because the volume[%s] occupies the mount path[%s] on host[%s]", msg.getVolumeUuid(), mountPath, hostUuid));
        }
    }

    private void checkMountPathOnHost(APIAttachDataVolumeToHostMsg msg, String attachVolumeErr) {
        List<String> mountPaths = Q.New(VolumeHostRefVO.class)
                .eq(VolumeHostRefVO_.hostUuid, msg.getHostUuid())
                .select(VolumeHostRefVO_.mountPath).listValues();
        if (mountPaths.contains(msg.getMountPath())) {
            throw new ApiMessageInterceptionException(operr(attachVolumeErr +
                    "because the another volume occupies the mount path[%s]", msg.getMountPath()));
        }
    }

    private void validate(APIDetachDataVolumeFromHostMsg msg) {
        if (!Q.New(VolumeHostRefVO.class).eq(VolumeHostRefVO_.volumeUuid, msg.getVolumeUuid()).isExists()) {
            throw new ApiMessageInterceptionException(operr("can not detach volume[%s] from host. " +
                    "it may have been detached", msg.getVolumeUuid()));
        }
    }

    private void populateExtensions() {
        for (MaxDataVolumeNumberExtensionPoint extp : pluginRgty.getExtensionList(MaxDataVolumeNumberExtensionPoint.class)) {
            MaxDataVolumeNumberExtensionPoint old = maxDataVolumeNumberExtensions.get(extp.getHypervisorTypeForMaxDataVolumeNumberExtension());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate MaxDataVolumeNumberExtensionPoint[%s, %s] for hypervisor type[%s]",
                        old.getClass().getName(), extp.getClass().getName(), extp.getHypervisorTypeForMaxDataVolumeNumberExtension())
                );
            }

            maxDataVolumeNumberExtensions.put(extp.getHypervisorTypeForMaxDataVolumeNumberExtension(), extp);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
