package org.zstack.sdnController.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

@PythonClassInventory
@Inventory(mappingVOClass = VxlanHostMappingVO.class, collectionValueOfMethod = "valueOf1")
public class VxlanHostMappingInventory implements Serializable {
    private String vxlanUuid;
    private String hostUuid;
    private Integer vlanId;
    private String physicalInterface;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VxlanHostMappingInventory() {}

    protected VxlanHostMappingInventory(VxlanHostMappingVO vo) {
        this.vxlanUuid = vo.getVxlanUuid();
        this.hostUuid = vo.getHostUuid();
        this.vlanId = vo.getVlanId();
        this.physicalInterface = vo.getPhysicalInterface();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static VxlanHostMappingInventory valueOf(VxlanHostMappingVO vo) {
        return new VxlanHostMappingInventory(vo);
    }

    public static VxlanHostMappingInventory valueOf2(Map<String,String> keyMap, Map<Integer,String> valueMap) {
        VxlanHostMappingInventory  inv = new VxlanHostMappingInventory();
        Optional<Map.Entry<String, String>> key = keyMap.entrySet().stream().findFirst();
        if (key.isPresent()) {
            inv.setHostUuid(key.get().getKey());
            inv.setPhysicalInterface(key.get().getValue());
        }
        Optional<Map.Entry<Integer, String>> value = valueMap.entrySet().stream().findFirst();
        if (value.isPresent()) {
            inv.setVlanId(value.get().getKey());
            inv.setPhysicalInterface(value.get().getValue());
        }
        return inv;
    }

    public static List<VxlanHostMappingInventory> valueOf1(Collection<VxlanHostMappingVO> vos) {
        List<VxlanHostMappingInventory> invs = new ArrayList<VxlanHostMappingInventory>(vos.size());
        for (VxlanHostMappingVO vo : vos) {
            invs.add(VxlanHostMappingInventory.valueOf(vo));
        }
        return invs;
    }

    public String getVxlanUuid() {
        return vxlanUuid;
    }

    public void setVxlanUuid(String vxlanUuid) {
        this.vxlanUuid = vxlanUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
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

