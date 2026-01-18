package org.zstack.header.core.external.service;

import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:25 AM
 */
@Entity
@Table
public class ExternalServiceConfigurationVO extends ResourceVO implements ToInventory {
    @Column
    private String serviceType;
    @Column
    private String configuration;
    @Column
    private String description;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getServiceType() {
        return serviceType;
    }

    @PreUpdate
    private void preUpdate() { lastOpDate = null; }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
