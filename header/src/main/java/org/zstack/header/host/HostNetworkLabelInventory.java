package org.zstack.header.host;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by boce.wang on 10/24/2024.
 */
@PythonClassInventory
@Inventory(mappingVOClass = HostNetworkLabelVO.class)
public class HostNetworkLabelInventory implements Serializable {

    private String uuid;

    private String serviceType;

    private Boolean system;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    protected HostNetworkLabelInventory(HostNetworkLabelVO vo) {
        this.setUuid(vo.getUuid());
        this.setServiceType(vo.getServiceType());
        this.setSystem(vo.getSystem());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public HostNetworkLabelInventory() {
    }

    public static HostNetworkLabelInventory valueOf(HostNetworkLabelVO vo) {
        return new HostNetworkLabelInventory(vo);
    }

    public static List<HostNetworkLabelInventory> valueOf(Collection<HostNetworkLabelVO> vos) {
        List<HostNetworkLabelInventory> invs = new ArrayList<>(vos.size());
        for (HostNetworkLabelVO vo : vos) {
            invs.add(HostNetworkLabelInventory.valueOf(vo));
        }
        return invs;
    }

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
