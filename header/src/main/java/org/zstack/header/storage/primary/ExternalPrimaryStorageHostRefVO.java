package org.zstack.header.storage.primary;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
public class ExternalPrimaryStorageHostRefVO extends PrimaryStorageHostRefVO {
    @Column
    private String protocol;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
