package org.zstack.header.host;

import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by boce.wang on 10/24/2024.
 */
@Entity
@Table
public class HostNetworkLabelVO implements ToInventory {
    @Id
    @Column
    private String uuid;

    @Column
    private String serviceType;

    @Column
    private Boolean system;

    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Boolean getSystem() {
        return system;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
