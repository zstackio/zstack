package org.zstack.xinfini;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.externalStorage.primary.ExternalStorageFencerType;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.volume.VolumeAfterExpungeExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.xinfini.XInfiniConstants;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class XInfiniStorageFactory implements ExternalPrimaryStorageSvcBuilder, BackupStorageSelector, VolumeAfterExpungeExtensionPoint {

    public static final ExternalStorageFencerType fencerType = new ExternalStorageFencerType(XInfiniConstants.IDENTITY, VolumeProtocol.iSCSI.toString());

    private List<String> preferBackupStorageTypes;

    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;

    @Override
    public PrimaryStorageControllerSvc buildControllerSvc(ExternalPrimaryStorageVO vo) {
        return new XInfiniStorageController(vo);
    }

    @Override
    public PrimaryStorageNodeSvc buildNodeSvc(ExternalPrimaryStorageVO vo) {
        return new XInfiniStorageController(vo);
    }

    @Override
    public void discover(String url, String config, ReturnValueCompletion<AddonInfo> completion) {
        // xinfini must set config
        if (StringUtils.isEmpty(config)) {
            completion.fail(operr(ORG_ZSTACK_XINFINI_10010, "empty config, cannot discover xinfini"));
            return;
        }

        XInfiniStorageController controller = new XInfiniStorageController(config);
        controller.connect(config, url, completion);
    }

    @Override
    public String getIdentity() {
        return XInfiniConstants.IDENTITY;
    }

    @Override
    public List<String> getPreferBackupStorageTypes() {
        return new ArrayList<>(preferBackupStorageTypes);
    }

    public void setPreferBackupStorageTypes(List<String> preferBackupStorageTypes) {
        this.preferBackupStorageTypes = preferBackupStorageTypes;
    }

    // TODO: hard code for less http call.
    @Override
    public void volumeAfterExpunge(VolumeInventory volume) {
        if (volume.getInstallPath() == null || !volume.getInstallPath().startsWith("xinfini://")) {
            return;
        }

        PrimaryStorageControllerSvc controller = extPsFactory.getControllerSvc(volume.getPrimaryStorageUuid());
        ((XInfiniStorageController) controller).cleanActiveRecord(volume);
    }
}
