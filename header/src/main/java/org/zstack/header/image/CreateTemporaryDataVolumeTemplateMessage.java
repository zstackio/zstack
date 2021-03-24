package org.zstack.header.image;

import java.util.List;

/**
 * Created by MaJin on 2021/3/18.
 */
public interface CreateTemporaryDataVolumeTemplateMessage extends CreateDataVolumeTemplateMessage {
    @Override
    default String getDescription() {
        return null;
    }

    @Override
    default List<String> getBackupStorageUuids() {
        return null;
    }

    @Override
    default String getResourceUuid() {
        return null;
    }

    @Override
    default void setBackupStorageUuids(List<String> backupStorageUuids) {}
}
