package org.zstack.network.hostNetwork.lldp.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = HostNetworkInterfaceLldpVO.class)
public class HostNetworkInterfaceLldpInventory implements Serializable {

    private String interfaceUuid;
    private String mode;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @Queryable(mappingClass = HostNetworkInterfaceLldpRefInventory.class, joinColumn = @JoinColumn(name = "interfaceUuid"))
    private HostNetworkInterfaceLldpRefInventory lldp;

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public HostNetworkInterfaceLldpRefInventory getLldp() {
        return lldp;
    }

    public void setLldp(HostNetworkInterfaceLldpRefInventory lldp) {
        this.lldp = lldp;
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

    public HostNetworkInterfaceLldpInventory() {

    }

    public HostNetworkInterfaceLldpInventory(HostNetworkInterfaceLldpVO vo) {
        this.interfaceUuid = vo.getInterfaceUuid();
        this.mode = vo.getMode();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
        this.lldp = (HostNetworkInterfaceLldpRefInventory.valueOf(vo.getLldpRefVO()));
    }

    public static HostNetworkInterfaceLldpInventory valueOf(HostNetworkInterfaceLldpVO vo) {
        return new HostNetworkInterfaceLldpInventory(vo);
    }

    public static List<HostNetworkInterfaceLldpInventory> valueOf(Collection<HostNetworkInterfaceLldpVO> vos) {
        return vos.stream().map(HostNetworkInterfaceLldpInventory::valueOf).collect(Collectors.toList());
    }
}
