package org.zstack.storage.fusionstor.backup;

import org.zstack.header.storage.backup.BackupStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/29/2015.
 */
@StaticMetamodel(FusionstorBackupStorageVO.class)
public class FusionstorBackupStorageVO_ extends BackupStorageVO_ {
    public static volatile SingularAttribute<FusionstorBackupStorageVO, String> fsid;
    public static volatile SingularAttribute<FusionstorBackupStorageVO, String> poolName;
}
