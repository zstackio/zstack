package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;

import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof ImageMessage) {
            ImageMessage imsg = (ImageMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, imsg.getImageUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddImageMsg) {
            validate((APIAddImageMsg)msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromRootVolumeMsg) {
            validate((APICreateRootVolumeTemplateFromRootVolumeMsg) msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromVolumeSnapshotMsg) {
            validate((APICreateRootVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeMsg) {
            validate((APICreateDataVolumeTemplateFromVolumeMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APICreateDataVolumeTemplateFromVolumeMsg msg) {
        VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (VolumeStatus.Ready != vol.getStatus()) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is not Ready, it's %s", vol.getUuid(), vol.getStatus()));
        }

        if (VolumeState.Enabled != vol.getState()) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is not Enabled, it's %s", vol.getUuid(), vol.getState()));
        }

        if (vol.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state);
            q.add(VmInstanceVO_.uuid, Op.EQ, vol.getVmInstanceUuid());
            VmInstanceState state = q.findValue();
            if (VmInstanceState.Stopped != state) {
                throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is attached to vm[uuid:%s]; the vm is not Stopped, it's %s",
                                vol.getUuid(), vol.getVmInstanceUuid(), state));
            }
        }


    }

    private void validate(APICreateRootVolumeTemplateFromVolumeSnapshotMsg msg) {
        if (msg.getPlatform() == null) {
            msg.setPlatform(ImagePlatform.Linux.toString());
        }
    }

    private void validate(APICreateRootVolumeTemplateFromRootVolumeMsg msg) {
        if (msg.getPlatform() == null) {
            msg.setPlatform(ImagePlatform.Linux.toString());
        }
    }

    private void validate(APIAddImageMsg msg) {
        if (ImageMediaType.ISO.toString().equals(msg.getMediaType())) {
            msg.setFormat(ImageConstant.ISO_FORMAT_STRING);
        }

        if (msg.isSystem() && (ImageMediaType.ISO.toString().equals(msg.getMediaType()) || ImageConstant.ISO_FORMAT_STRING.equals(msg.getFormat()))) {
            throw new ApiMessageInterceptionException(argerr(
                    "ISO cannot be used as system image"
            ));
        }

        if (!VolumeFormat.hasType(msg.getFormat())) {
            throw new ApiMessageInterceptionException(argerr("unknown format[%s]", msg.getFormat()));
        }

        if (msg.getType() != null && !ImageType.hasType(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("unsupported image type[%s]", msg.getType()));
        }

        if (msg.getMediaType() == null) {
            msg.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        }

        if (msg.getPlatform() == null) {
            msg.setPlatform(ImagePlatform.Linux.toString());
        }

        if (msg.getBackupStorageUuids() != null) {
            SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
            q.select(BackupStorageVO_.uuid);
            q.add(BackupStorageVO_.status, Op.EQ, BackupStorageStatus.Connected);
            q.add(BackupStorageVO_.state, Op.EQ, BackupStorageState.Enabled);
            q.add(BackupStorageVO_.uuid, Op.IN, msg.getBackupStorageUuids());
            List<String> bsUuids = q.listValue();
            if (bsUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(operr("no backup storage specified in uuids%s is available for adding this image; they are not in status %s or not in state %s, or the uuid is invalid backup storage uuid",
                                msg.getBackupStorageUuids(), BackupStorageStatus.Connected, BackupStorageState.Enabled));
            }
            msg.setBackupStorageUuids(bsUuids);
        }

        // compatible with file:/// and /
        if (msg.getUrl().startsWith("/")) {
            msg.setUrl(String.format("file://%s", msg.getUrl()));
        } else if (!msg.getUrl().startsWith("file:///") && !msg.getUrl().startsWith("http://") && !msg.getUrl().startsWith("https://")) {
            throw new ApiMessageInterceptionException(argerr("url must starts with 'file:///', 'http://', 'https://' or '/'"));
        }
    }
}
