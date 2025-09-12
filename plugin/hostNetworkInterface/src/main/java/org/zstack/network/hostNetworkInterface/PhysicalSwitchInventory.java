package org.zstack.network.hostNetworkInterface;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Physical Switch Inventory
 */
@Inventory(mappingVOClass = PhysicalSwitchVO.class)
@PythonClassInventory
public class PhysicalSwitchInventory implements Serializable {
    
    private String uuid;
    private String name;
    private String description;
    private String ip;
    private String mac;
    private String mode;
    private String softwareVersion;
    private String sdnControllerUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<PhysicalSwitchPortInventory> ports;

    public static PhysicalSwitchInventory valueOf(PhysicalSwitchVO vo) {
        PhysicalSwitchInventory inv = new PhysicalSwitchInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setIp(vo.getIp());
        inv.setMac(vo.getMac());
        inv.setMode(vo.getMode());
        inv.setSoftwareVersion(vo.getSoftwareVersion());
        inv.setSdnControllerUuid(vo.getSdnControllerUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setPorts(PhysicalSwitchPortInventory.valueOf(vo.getPorts()));
        return inv;
    }

    public static List<PhysicalSwitchInventory> valueOf(Collection<PhysicalSwitchVO> vos) {
        List<PhysicalSwitchInventory> invs = new ArrayList<>();
        for (PhysicalSwitchVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
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

    public List<PhysicalSwitchPortInventory> getPorts() {
        return ports;
    }

    public void setPorts(List<PhysicalSwitchPortInventory> ports) {
        this.ports = ports;
    }

    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }
}
