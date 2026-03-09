package org.zstack.storage.ceph.primary;

/**
 * Created by frank on 7/29/2015.
 */

import org.zstack.storage.ceph.CephMonAO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(CephPrimaryStorageMonVO.class)
public class CephPrimaryStorageMonVO_ extends CephMonAO_ {
    public static volatile SingularAttribute<CephPrimaryStorageMonVO, String> primaryStorageUuid;
}
