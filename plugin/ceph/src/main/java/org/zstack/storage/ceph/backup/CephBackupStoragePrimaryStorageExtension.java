package org.zstack.storage.ceph.backup;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStoragePrimaryStorageExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.storage.ceph.CephConstants;

import java.util.List;

/**
 * Created by mingjian.deng on 2017/10/31.
 */
public class CephBackupStoragePrimaryStorageExtension implements BackupStoragePrimaryStorageExtensionPoint {
    @Override
    public String getPrimaryStoragePriorityMap(BackupStorageInventory bs, ImageInventory image) {
        if (!bs.getType().equals(CephConstants.CEPH_BACKUP_STORAGE_TYPE)) {
            return null;
        }
        return CephBackupStorageGlobalProperty.CEPH_BACKUPSTORAGE_PRIORITY;
    }

    @Override
    public List<String> getBackupStorageSupportedPS(String psUuid) {
        PrimaryStorageInventory ps = PrimaryStorageInventory.valueOf((PrimaryStorageVO)
                Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, psUuid).find());
        if(!ps.getType().equals(CephConstants.CEPH_PRIMARY_STORAGE_TYPE)){
            return null;
        }
        return SQL.New("select b.uuid from CephPrimaryStorageVO c, PrimaryStorageVO p, CephBackupStorageVO b, BackupStorageZoneRefVO ref where " +
                "p.uuid = :puuid and c.uuid = p.uuid and b.fsid = c.fsid and b.uuid = ref.backupStorageUuid and ref.zoneUuid = p.zoneUuid").
                param("puuid", psUuid).list();
    }

    @Override
    public void cleanupPrimaryCacheForBS(PrimaryStorageInventory ps, String hostUuid, Completion completion) {
        completion.success();
    }
}
