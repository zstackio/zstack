package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
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
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;

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
    @Autowired
    private PluginRegistry pluginRgty;

    private static final String[] allowedProtocols = new String[]{
            "http://",
            "https://",
            "file:///",
            "upload://",
            "zstore://"
    };

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
    }

    private void validate(APICreateRootVolumeTemplateFromVolumeSnapshotMsg msg) {
        if (msg.getPlatform() == null) {
            msg.setPlatform(ImagePlatform.Linux.toString());
        }
    }

    private void validate(APICreateRootVolumeTemplateFromRootVolumeMsg msg) {
        if (msg.getPlatform() == null) {
            String platform = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.rootVolumeUuid, msg.getRootVolumeUuid()).select(VmInstanceVO_.platform).findValue();
            msg.setPlatform(platform == null ? ImagePlatform.Linux.toString() : platform);
        }

        if (msg.getGuestOsType() == null) {
            List<String> osTypes = SQL.New("select i.guestOsType from VolumeVO v, ImageVO i where v.uuid=:vol and v.rootImageUuid = i.uuid").
                    param("vol", msg.getRootVolumeUuid()).list();
            if (osTypes != null && osTypes.size() > 0) {
                msg.setGuestOsType(osTypes.get(0));
            }
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
            isValidBS(bsUuids);
            msg.setBackupStorageUuids(bsUuids);
        }

        // compatible with file:/// and /
        if (msg.getUrl().startsWith("/")) {
            msg.setUrl(String.format("file://%s", msg.getUrl()));
        } else if (!isValidProtocol(msg.getUrl())) {
            throw new ApiMessageInterceptionException(argerr("url must starts with 'file:///', 'http://', 'https://' or '/'"));
        }
    }


    private void isValidBS(List<String> bsUuids) {
        for (AddImageExtensionPoint ext : pluginRgty.getExtensionList(AddImageExtensionPoint.class)) {
            ext.validateAddImage(bsUuids);
        }
    }

    private static boolean isValidProtocol(String url) {
        for (String p : allowedProtocols) {
            if (url.startsWith(p)) {
                return true;
            }
        }

        return false;
    }
}
