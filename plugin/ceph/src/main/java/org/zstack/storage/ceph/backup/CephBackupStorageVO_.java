package org.zstack.storage.ceph.backup;

import org.zstack.header.storage.backup.BackupStorageVO_;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/29/2015.
 */
@StaticMetamodel(CephBackupStorageVO.class)
public class CephBackupStorageVO_ extends BackupStorageVO_ {
    public static volatile SingularAttribute<CephBackupStorageVO, String> fsid;
    public static volatile SingularAttribute<CephBackupStorageVO, String> poolName;
    public static volatile SingularAttribute<CephBackupStorageVO, Integer> poolReplicatedSize;
    public static volatile SingularAttribute<CephBackupStorageVO, Long> poolUsedCapacity;
    public static volatile SingularAttribute<CephBackupStorageVO, Long> poolAvailableCapacity;
}
