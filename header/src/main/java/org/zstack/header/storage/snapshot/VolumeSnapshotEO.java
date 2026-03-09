package org.zstack.header.storage.snapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 */
@Entity
@Table
public class VolumeSnapshotEO extends VolumeSnapshotAO {
    @Column
    private String deleted;

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }
}
