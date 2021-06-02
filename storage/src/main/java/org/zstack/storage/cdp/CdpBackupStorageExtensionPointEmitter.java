package org.zstack.storage.cdp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.storage.cdp.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageException;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageStateEvent;
import org.zstack.header.storage.backup.BackupStorageState;

import java.util.List;

class CdpBackupStorageExtensionPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(CdpBackupStorageExtensionPointEmitter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    void beforeDelete(final BackupStorageInventory inv) {
    }


    void afterDelete(final BackupStorageInventory inv) {
    }

    void preChange(BackupStorageVO vo, BackupStorageStateEvent evt) throws BackupStorageException {
        BackupStorageState next = AbstractCdpBackupStorage.getNextState(vo.getState(), evt);
        BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
    }

    void beforeChange(BackupStorageVO vo, final BackupStorageStateEvent evt) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        final BackupStorageState next = AbstractCdpBackupStorage.getNextState(vo.getState(), evt);
    }


    void afterChange(BackupStorageVO vo, final BackupStorageStateEvent evt, final BackupStorageState preState) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
    }

    String preAttach(BackupStorageVO vo, String zoneUuid) {
        BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);

        return null;
    }

    void beforeAttach(BackupStorageVO vo, final String zoneUuid) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
    }

    void failToAttach(BackupStorageVO vo, final String zoneUuid) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
    }


    void afterAttach(BackupStorageVO vo, final String zoneUuid) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
    }

    void preDetach(BackupStorageVO vo, String zoneUuid) throws BackupStorageException {
        BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
    }

    void beforeDetach(BackupStorageVO vo, final String zoneUuid) {
    }

    void failToDetach(BackupStorageVO vo, final String zoneUuid) {
    }

    void afterDetach(BackupStorageVO vo, final String zoneUuid) {
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
