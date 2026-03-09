package org.zstack.header.configuration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 */
@Entity
@Table
public class InstanceOfferingEO extends InstanceOfferingAO {
    @Column
    private String deleted;

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }
}
