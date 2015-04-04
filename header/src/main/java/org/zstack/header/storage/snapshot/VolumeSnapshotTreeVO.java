package org.zstack.header.storage.snapshot;

import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 */
@Entity
@Table
@EO(EOClazz = VolumeSnapshotTreeEO.class)
public class VolumeSnapshotTreeVO extends VolumeSnapshotTreeAO {
}
