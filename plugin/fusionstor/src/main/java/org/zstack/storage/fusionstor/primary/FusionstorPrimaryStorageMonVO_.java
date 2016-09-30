package org.zstack.storage.fusionstor.primary;

/**
 * Created by frank on 7/29/2015.
 */

import org.zstack.storage.fusionstor.FusionstorMonAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(FusionstorPrimaryStorageMonVO.class)
public class FusionstorPrimaryStorageMonVO_ extends FusionstorMonAO_ {
    public static volatile SingularAttribute<FusionstorPrimaryStorageMonVO, String> primaryStorageUuid;
}
