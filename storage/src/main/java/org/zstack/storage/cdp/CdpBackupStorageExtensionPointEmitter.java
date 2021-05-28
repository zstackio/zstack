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

    private List<CdpBackupStorageAttachExtensionPoint> attachExts;
    private List<CdpBackupStorageDetachExtensionPoint> detachExts;
    private List<CdpBackupStorageDeleteExtensionPoint> delExts;
    private List<CdpBackupStorageChangeStateExtensionPoint> changeExts;

    void beforeDelete(final BackupStorageInventory inv) {
        CollectionUtils.safeForEach(delExts, new ForEachFunction<CdpBackupStorageDeleteExtensionPoint>() {
            @Override
            public void run(CdpBackupStorageDeleteExtensionPoint arg) {
                arg.beforeDeleteSecondaryStorage(inv);
            }
        });
    }


    void afterDelete(final BackupStorageInventory inv) {
        CollectionUtils.safeForEach(delExts, new ForEachFunction<CdpBackupStorageDeleteExtensionPoint>() {
            @Override
            public void run(CdpBackupStorageDeleteExtensionPoint arg) {
                arg.afterDeleteSecondaryStorage(inv);
            }
        });
    }

    void preChange(BackupStorageVO vo, BackupStorageStateEvent evt) throws BackupStorageException {
        BackupStorageState next = AbstractCdpBackupStorage.getNextState(vo.getState(), evt);
        BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        for (CdpBackupStorageChangeStateExtensionPoint extp : changeExts) {
            try {
                extp.preChangeSecondaryStorageState(inv, evt, next);
            } catch(BackupStorageException be) {
                logger.debug(String.format("%s refused to change backup storage[uuid:%s] state from %s to %s because %s", extp.getClass().getCanonicalName(), vo.getUuid(), vo.getState(), next, be.getMessage()));
                throw be;
            } catch (Exception e) {
                logger.warn("Exception happened when calling preChangeCdpBackupStorageState of CdpBackupStorageChangeStateExtensionPoint", e);
            }
        }
    }

    void beforeChange(BackupStorageVO vo, final BackupStorageStateEvent evt) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        final BackupStorageState next = AbstractCdpBackupStorage.getNextState(vo.getState(), evt);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<CdpBackupStorageChangeStateExtensionPoint>() {
            @Override
            public void run(CdpBackupStorageChangeStateExtensionPoint arg) {
                arg.beforeChangeSecondaryStorageState(inv, evt, next);
            }
        });
    }


    void afterChange(BackupStorageVO vo, final BackupStorageStateEvent evt, final BackupStorageState preState) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<CdpBackupStorageChangeStateExtensionPoint>() {
            @Override
            public void run(CdpBackupStorageChangeStateExtensionPoint arg) {
                arg.afterChangeSecondaryStorageState(inv, evt, preState);
            }
        });
    }

    String preAttach(BackupStorageVO vo, String zoneUuid) {
        BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        for (CdpBackupStorageAttachExtensionPoint extp : attachExts) {
            try {
                String reason = extp.preAttachCdpBackupStorage(inv, zoneUuid);
                if (reason != null) {
                    logger.debug(String.format("%s refused to attach backup storage[uuid:%s] because %s", extp.getClass().getName(), inv.getUuid(), reason));
                    return reason;
                }
            } catch (Exception e) {
                logger.warn("Exception happened when calling preAttach of CdpBackupStorageAttachExtensionPoint", e);
            }
        }

        return null;
    }

    void beforeAttach(BackupStorageVO vo, final String zoneUuid) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, new ForEachFunction<CdpBackupStorageAttachExtensionPoint>() {
            @Override
            public void run(CdpBackupStorageAttachExtensionPoint arg) {
                arg.beforeAttachCdpBackupStorage(inv, zoneUuid);
            }
        });
    }

    void failToAttach(BackupStorageVO vo, final String zoneUuid) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, new ForEachFunction<CdpBackupStorageAttachExtensionPoint>() {
            @Override
            public void run(CdpBackupStorageAttachExtensionPoint arg) {
                arg.failToAttachCdpBackupStorage(inv, zoneUuid);
            }
        });
    }


    void afterAttach(BackupStorageVO vo, final String zoneUuid) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, new ForEachFunction<CdpBackupStorageAttachExtensionPoint>() {
            @Override
            public void run(CdpBackupStorageAttachExtensionPoint arg) {
                arg.afterAttachCdpBackupStorage(inv, zoneUuid);
            }
        });
    }

    void preDetach(BackupStorageVO vo, String zoneUuid) throws BackupStorageException {
        BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        for (CdpBackupStorageDetachExtensionPoint extp : detachExts) {
            try {
                extp.preDetachCdpBackupStorage(inv, zoneUuid);
            } catch (BackupStorageException be) {
                logger.debug(String.format("%s refused to detach backup storage[uuid:%s] because %s", extp.getClass().getName(), inv.getUuid(), be.getMessage()));
                throw be;
            } catch (Exception e) {
                logger.warn("Exception happened when calling preDetach of CdpBackupStorageDetachExtensionPoint", e);
            }
        }
    }

    void beforeDetach(BackupStorageVO vo, final String zoneUuid) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, new ForEachFunction<CdpBackupStorageDetachExtensionPoint>() {
            @Override
            public void run(CdpBackupStorageDetachExtensionPoint arg) {
                arg.beforeDetachCdpBackupStorage(inv, zoneUuid);
            }
        });
    }

    void failToDetach(BackupStorageVO vo, final String zoneUuid) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, new ForEachFunction<CdpBackupStorageDetachExtensionPoint>() {
            @Override
            public void run(CdpBackupStorageDetachExtensionPoint arg) {
                arg.afterDetachCdpBackupStorage(inv, zoneUuid);
            }
        });
    }

    void afterDetach(BackupStorageVO vo, final String zoneUuid) {
        final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, new ForEachFunction<CdpBackupStorageDetachExtensionPoint>() {
            @Override
            public void run(CdpBackupStorageDetachExtensionPoint arg) {
                arg.afterDetachCdpBackupStorage(inv, zoneUuid);
            }
        });
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        attachExts = pluginRgty.getExtensionList(CdpBackupStorageAttachExtensionPoint.class);
        detachExts = pluginRgty.getExtensionList(CdpBackupStorageDetachExtensionPoint.class);
        delExts = pluginRgty.getExtensionList(CdpBackupStorageDeleteExtensionPoint.class);
        changeExts = pluginRgty.getExtensionList(CdpBackupStorageChangeStateExtensionPoint.class);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
