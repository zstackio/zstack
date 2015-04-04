package org.zstack.header.storage.snapshot;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(VolumeSnapshotTreeAO.class)
public class VolumeSnapshotTreeAO_ {
    public static volatile SingularAttribute<VolumeSnapshotTreeAO, String> uuid;
    public static volatile SingularAttribute<VolumeSnapshotTreeAO, Boolean> current;
    public static volatile SingularAttribute<VolumeSnapshotTreeAO, String> volumeUuid;
    public static volatile SingularAttribute<VolumeSnapshotTreeAO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeSnapshotTreeAO, Timestamp> lastOpDate;
}
