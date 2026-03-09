package org.zstack.header.storage.snapshot;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(VolumeSnapshotEO.class)
public class VolumeSnapshotEO_ extends VolumeSnapshotAO_ {
    public static volatile SingularAttribute<VolumeSnapshotEO, String> deleted;
}
