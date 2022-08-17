package org.zstack.sdnController.header;

import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
public class PortLldpInfoVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = L2NetworkVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String l2NetworkUuid;

    // server interface name
    @Column
    private String interfaceName;
    // server interface type: bond or physical
    @Column
    private String interfaceType;
    // server bond interface name
    @Column
    private String bondIfName;
    // switch port name
    @Column
    private String portName;
    // switch system name
    @Column
    private String systemName;
    // switch mac address
    @Column
    private String chassisMac;
    // whether the switch port is configured with aggregation
    @Column
    private Boolean aggregated;
    // aggregated port ID if the switch port is configured with aggregation
    @Column
    private Integer aggregatedPortID;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getBondIfName() {
        return bondIfName;
    }

    public void setBondIfName(String bondIfName) {
        this.bondIfName = bondIfName;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getChassisMac() {
        return chassisMac;
    }

    public void setChassisMac(String chassisMac) {
        this.chassisMac = chassisMac;
    }

    public Boolean getAggregated() {
        return aggregated;
    }

    public void setAggregated(Boolean aggregated) {
        this.aggregated = aggregated;
    }

    public Integer getAggregatedPortID() {
        return aggregatedPortID;
    }

    public void setAggregatedPortID(Integer aggregatedPortID) {
        this.aggregatedPortID = aggregatedPortID;
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
