package org.zstack.storage.backup.sftp;

import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStoragePrimaryStorageExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.util.List;

public class SftpBackupStoragePrimaryStorageExtension implements BackupStoragePrimaryStorageExtensionPoint {

    @Override
    public String getPrimaryStoragePriorityMap(BackupStorageInventory bs, ImageInventory image) {
        if (!bs.getType().equals(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE)) {
            return null;
        }
        return SftpBackupStorageGlobalProperty.SFTP_BACKUPSTORAGE_PRIORITY;
    }

    @Override
    public List<String> getBackupStorageSupportedPS(String psUuid) {
        return SQL.New("select b.uuid from PrimaryStorageVO p, SftpBackupStorageVO b, BackupStorageZoneRefVO ref where " +
                "p.uuid = :puuid and b.uuid = ref.backupStorageUuid and ref.zoneUuid = p.zoneUuid and b.type = :btype").
                param("puuid", psUuid).param("btype", SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE).list();
    }

    @Override
    public void cleanupPrimaryCacheForBS(PrimaryStorageInventory ps, String hostUuid, Completion completion) {
        completion.success();
    }
}
