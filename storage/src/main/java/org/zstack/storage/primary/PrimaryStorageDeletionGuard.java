package org.zstack.storage.primary;

import org.zstack.core.db.Q;
import org.zstack.core.db.GLock;
import org.zstack.core.db.SQL;
import org.zstack.core.Platform;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.core.jsonlabel.JsonLabelVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageConstants;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

public final class PrimaryStorageDeletionGuard {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageDeletionGuard.class);
    private static final String USING = "primaryStorageDeletionGuard::using::";

    static GLock lock(String storageUuid) {
        return new GLock("primary-storage-delete-guard-" + storageUuid, 30);
    }

    public static Long acquire(String storageUuid, String volumeUuid) {
        GLock lock = lock(storageUuid);
        lock.lock();
        try {
            PrimaryStorageVO storage = Q.New(PrimaryStorageVO.class)
                    .eq(PrimaryStorageVO_.uuid, storageUuid).find();
            if (storage == null) {
                throw new OperationFailureException(operr("primary storage[uuid:%s] no longer exists", storageUuid));
            }
            if (PrimaryStorageConstants.LOCAL_STORAGE_TYPE.equals(storage.getType())) {
                return null;
            }
            if (storage.getState() == PrimaryStorageState.Deleting) {
                throw new OperationFailureException(operr("cannot create volume[uuid:%s]: primary storage[uuid:%s] is being deleted",
                        volumeUuid, storageUuid));
            }
            return new JsonLabel().create(USING + Platform.getUuid(),
                    Platform.getManagementServerId() + "::" + volumeUuid, storageUuid).getId();
        } finally {
            lock.unlock();
        }
    }

    public static void release(Long lease) {
        if (lease != null) {
            try {
                SQL.New(JsonLabelVO.class).eq(JsonLabelVO_.id, lease).hardDelete();
            } catch (RuntimeException e) {
                // Do not turn an already committed volume result into failure
                // or strand its task queue. A remaining guard keeps deletion blocked.
                logger.warn(String.format("failed to release primary storage operation guard[id:%s]; verify it manually", lease), e);
            }
        }
    }

    static ErrorCode checkVolumes(PrimaryStorageVO storage) {
        if (PrimaryStorageConstants.LOCAL_STORAGE_TYPE.equals(storage.getType())) {
            return null;
        }

        // Deleted volumes remain recoverable until expunged. Detached and
        // shareable volumes also protect their storage, regardless of VM state.
        VolumeVO volume = Q.New(VolumeVO.class)
                .eq(VolumeVO_.primaryStorageUuid, storage.getUuid()).limit(1).find();
        if (volume == null) {
            JsonLabelVO guard = Q.New(JsonLabelVO.class).eq(JsonLabelVO_.resourceUuid, storage.getUuid())
                    .like(JsonLabelVO_.labelKey, USING + "%").limit(1).find();
            if (guard == null) {
                return null;
            }
            // A missing MN heartbeat does not fence its asynchronous work.
            // Keep abandoned guards until an operator confirms it has stopped.
            return operr("cannot delete non-local primary storage[uuid:%s]: unfinished volume operation guard[id:%s, owner/volume:%s]." +
                            " Wait for completion. For an abandoned operation, first confirm the old process and its pending operations have stopped;" +
                            " inspect SELECT id, labelKey, labelValue, resourceUuid FROM JsonLabelVO WHERE id=%s;" +
                            " then remove only DELETE FROM JsonLabelVO WHERE id=%s AND resourceUuid='%s' AND labelKey='%s' AND labelValue='%s'",
                    storage.getUuid(), guard.getId(), guard.getLabelValue(), guard.getId(), guard.getId(),
                    storage.getUuid(), guard.getLabelKey(), guard.getLabelValue());
        }

        return operr("cannot delete non-local primary storage[name:%s, uuid:%s, type:%s]:" +
                        " volume[name:%s, uuid:%s, type:%s, status:%s] still exists on it." +
                        " Migrate the volume to another primary storage or delete and expunge it before deleting this storage",
                storage.getName(), storage.getUuid(), storage.getType(),
                volume.getName(), volume.getUuid(), volume.getType(), volume.getStatus());
    }
}
