package org.zstack.header.storage.snapshot;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(VolumeSnapshotTreeEO.class)
public class VolumeSnapshotTreeEO_ extends VolumeSnapshotTreeAO_ {
    public static volatile SingularAttribute<VolumeSnapshotTreeEO, String> deleted;
}
