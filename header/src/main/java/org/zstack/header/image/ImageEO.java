package org.zstack.header.image;

import jakarta.persistence.*;

/**
 */
@Entity
@Table
public class ImageEO extends ImageAO {
    @Column
    private String deleted;

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }
}
