package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageState;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import javax.persistence.Tuple;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
        }

        setServiceId(msg);
        return msg;
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


        if (vol.getType() == VolumeType.Root) {
            throw new ApiMessageInterceptionException(operr("the volume[uuid:%s, name:%s] is Root Volume, can't detach it",
                            vol.getUuid(), vol.getName()));
        }
    }

    private void validate(APIAttachDataVolumeToVmMsg msg) {

        new SQLBatch(){
            @Override
            protected void scripts() {
                long count = sql("select count(vm.uuid)" +
                        " from VmInstanceVO vm, ImageVO image" +
                        " where vm.uuid = :vmUuid" +
                        " and vm.imageUuid = image.uuid" +
                        " and image.platform = :platformType" +
                        " and vm.state != :vmState")
                        .param("vmUuid",msg.getVmInstanceUuid())
                        .param("vmState", VmInstanceState.Stopped)
                        .param("platformType", ImagePlatform.Other).find();
                if(count > 0){
                   throw new ApiMessageInterceptionException(operr("the vm[uuid:%s] doesn't support to online attach volume[%s] on the basis of that the image platform type of the vm is other ", msg.getVmInstanceUuid(), msg.getVolumeUuid()));
                }

                VolumeVO vol = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
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

                if (vol.getVmInstanceUuid() != null) {
                    throw new ApiMessageInterceptionException(operr("data volume[%s] has been attached to vm[uuid:%s], can't attach again",
                            vol.getUuid(), vol.getVmInstanceUuid()));
                }

                if (VolumeStatus.Ready != vol.getStatus() && VolumeStatus.NotInstantiated != vol.getStatus()) {
                    throw new ApiMessageInterceptionException(operr("data volume can only be attached when status is [%s, %s], current is %s",
                            VolumeStatus.Ready, VolumeStatus.NotInstantiated, vol.getStatus()));
                }

                String hvType = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid,msg.getVmInstanceUuid()).select(VmInstanceVO_.hypervisorType).findValue();
                if (vol.getFormat() != null) {
                    HypervisorType volHvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(vol.getFormat());
                    if (!hvType.equals(volHvType.toString())) {
                        throw new ApiMessageInterceptionException(operr("data volume[uuid:%s] has format[%s] that can only be attached to hypervisor[%s], but vm[uuid:%s] has hypervisor type[%s]. Can't attach",
                                vol.getUuid(), vol.getFormat(), volHvType, msg.getVmInstanceUuid(), hvType));
                    }
                }

                MaxDataVolumeNumberExtensionPoint ext = maxDataVolumeNumberExtensions.get(hvType);
                int maxDataVolumeNum = DEFAULT_MAX_DATA_VOLUME_NUMBER;
                if (ext != null) {
                    maxDataVolumeNum = ext.getMaxDataVolumeNumber();
                }

                count = Q.New(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data).eq(VolumeVO_.vmInstanceUuid, msg.getVolumeUuid()).count();
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
        if (type == VolumeType.Root) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] is Root volume, can't be deleted", msg.getVolumeUuid()));
        }

        VolumeStatus status = t.get(1, VolumeStatus.class);
        if (status == VolumeStatus.Deleted) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is already in status of deleted", msg.getVolumeUuid()));
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
