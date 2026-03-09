package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
public class ExternalPrimaryStorageHostRefVO extends PrimaryStorageHostRefVO {
    @Column
    private String protocol;

    @Column
    private int hostId;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getHostId() {
        return hostId;
    }

    public void setHostId(int hostId) {
        this.hostId = hostId;
    }
}
