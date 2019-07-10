package org.zstack.storage.surfs.backup;
import org.zstack.storage.surfs.SurfsNodeAO_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/29/2015.
 */
@StaticMetamodel(SurfsBackupStorageNodeVO.class)
public class SurfsBackupStorageNodeVO_ extends SurfsNodeAO_ {
    public static volatile SingularAttribute<SurfsBackupStorageNodeVO, String> backupStorageUuid;
}
