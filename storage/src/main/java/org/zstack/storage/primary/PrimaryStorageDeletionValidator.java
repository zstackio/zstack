package org.zstack.storage.primary;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.primary.PrimaryStorageConstants;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import static org.zstack.core.Platform.operr;

final class PrimaryStorageDeletionValidator {
    static ErrorCode checkVolumes(PrimaryStorageVO storage) {
        if (PrimaryStorageConstants.LOCAL_STORAGE_TYPE.equals(storage.getType())) {
            return null;
        }

        // Deleted volumes remain recoverable until expunged. Detached and
        // shareable volumes also protect their storage, regardless of VM state.
        VolumeVO volume = Q.New(VolumeVO.class)
                .eq(VolumeVO_.primaryStorageUuid, storage.getUuid()).limit(1).find();
        if (volume == null) {
            return null;
        }

        return operr("cannot delete non-local primary storage[name:%s, uuid:%s, type:%s]:" +
                        " volume[name:%s, uuid:%s, type:%s, status:%s] still exists on it." +
                        " Migrate the volume to another primary storage or delete and expunge it before deleting this storage",
                storage.getName(), storage.getUuid(), storage.getType(),
                volume.getName(), volume.getUuid(), volume.getType(), volume.getStatus());
    }
}
