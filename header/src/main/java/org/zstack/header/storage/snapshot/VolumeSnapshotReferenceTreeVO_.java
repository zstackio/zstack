package org.zstack.header.storage.snapshot;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;


@StaticMetamodel(VolumeSnapshotReferenceTreeVO.class)
public class VolumeSnapshotReferenceTreeVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VolumeSnapshotReferenceTreeVO, String> rootImageUuid;
}
