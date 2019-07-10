package org.zstack.storage.surfs.backup;

import org.zstack.header.storage.backup.BackupStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/29/2015.
 */
@StaticMetamodel(SurfsBackupStorageVO.class)
public class SurfsBackupStorageVO_ extends BackupStorageVO_ {
    public static volatile SingularAttribute<SurfsBackupStorageVO, String> fsid;
    public static volatile SingularAttribute<SurfsBackupStorageVO, String> poolName;
}
