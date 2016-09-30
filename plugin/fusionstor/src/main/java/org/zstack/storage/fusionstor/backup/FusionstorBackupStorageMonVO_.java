package org.zstack.storage.fusionstor.backup;

import org.zstack.storage.fusionstor.FusionstorMonAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/29/2015.
 */
@StaticMetamodel(FusionstorBackupStorageMonVO.class)
public class FusionstorBackupStorageMonVO_ extends FusionstorMonAO_ {
    public static volatile SingularAttribute<FusionstorBackupStorageMonVO, String> backupStorageUuid;
}
