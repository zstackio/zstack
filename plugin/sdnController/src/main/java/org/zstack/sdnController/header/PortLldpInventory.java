package org.zstack.sdnController.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = PortLldpInfoVO.class, collectionValueOfMethod = "valueOf1")
public class PortLldpInventory implements Serializable {
    private String uuid;
    private String l2NetworkUuid;
    private String interfaceName;
    private String interfaceType;
    private String bondIfName;
    private String portName;
    private String systemName;
    private String chassisMac;
    private Boolean aggregated;
    private Integer aggregatedPortID;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public PortLldpInventory() {}

    protected PortLldpInventory(PortLldpInfoVO vo) {
        this.uuid = vo.getUuid();
        this.l2NetworkUuid = vo.getL2NetworkUuid();
        this.interfaceName = vo.getInterfaceName();
        this.interfaceType = vo.getInterfaceType();
        this.bondIfName = vo.getBondIfName();
        this.portName = vo.getPortName();
        this.systemName = vo.getSystemName();
        this.chassisMac = vo.getChassisMac();
        this.aggregated = vo.getAggregated();
        this.aggregatedPortID = vo.getAggregatedPortID();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }


    public static PortLldpInventory valueOf(PortLldpInfoVO vo) {
        return new PortLldpInventory(vo);
    }

    public static List<PortLldpInventory> valueOf1(Collection<PortLldpInfoVO> vos) {
        List<PortLldpInventory> invs = new ArrayList<PortLldpInventory>(vos.size());
        for (PortLldpInfoVO vo : vos) {
            invs.add(PortLldpInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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