package org.zstack.sdnController.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        if (keyMap.entrySet().stream().findFirst().isPresent()) {
            inv.setHostUuid(keyMap.entrySet().stream().findFirst().get().getKey());
            inv.setPhysicalInterface(keyMap.entrySet().stream().findFirst().get().getValue());
        }
        if (valueMap.entrySet().stream().findFirst().isPresent()) {
            inv.setVlanId(valueMap.entrySet().stream().findFirst().get().getKey());
            inv.setPhysicalInterface(valueMap.entrySet().stream().findFirst().get().getValue());
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

