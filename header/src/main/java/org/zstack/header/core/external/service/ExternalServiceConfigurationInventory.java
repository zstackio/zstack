package org.zstack.header.core.external.service;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:31 AM
 */
@Inventory(mappingVOClass = ExternalServiceConfigurationVO.class)
public class ExternalServiceConfigurationInventory {
    private String uuid;
    private String serviceType;
    private String configuration;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ExternalServiceConfigurationInventory valueOf(ExternalServiceConfigurationVO vo) {
        ExternalServiceConfigurationInventory inv = new ExternalServiceConfigurationInventory();
        inv.setUuid(vo.getUuid());
        inv.setDescription(vo.getDescription());
        inv.setServiceType(vo.getServiceType());
        inv.setConfiguration(vo.getConfiguration());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ExternalServiceConfigurationInventory> valueOf(Collection<ExternalServiceConfigurationVO> vos) {
        List<ExternalServiceConfigurationInventory> invs = new ArrayList<ExternalServiceConfigurationInventory>();
        for (ExternalServiceConfigurationVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
