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
@Inventory(mappingVOClass = VxlanMappingVO.class, collectionValueOfMethod = "valueOf1")
public class VxlanMappingInventory implements Serializable {
    private Integer vni;
    private String hostUuid;
    private Integer vlanId;
    private String physicalInterface;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VxlanMappingInventory() {}

    protected VxlanMappingInventory(VxlanMappingVO vo) {
        this.vni = vo.getVni();
        this.hostUuid = vo.getHostUuid();
        this.vlanId = vo.getVlanId();
        this.physicalInterface = vo.getPhysicalInterface();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static VxlanMappingInventory valueOf(VxlanMappingVO vo) {
        return new VxlanMappingInventory(vo);
    }

    public static VxlanMappingInventory valueOf2(Map<Integer,String> keyMap, Map<Integer,String> valueMap) {
        VxlanMappingInventory  inv = new VxlanMappingInventory();
        inv.setVni(keyMap.entrySet().stream().findFirst().get().getKey());
        inv.setPhysicalInterface(keyMap.entrySet().stream().findFirst().get().getValue());
        inv.setVlanId(valueMap.entrySet().stream().findFirst().get().getKey());
        inv.setPhysicalInterface(valueMap.entrySet().stream().findFirst().get().getValue());
        return inv;
    }

    public static List<VxlanMappingInventory> valueOf1(Collection<VxlanMappingVO> vos) {
        List<VxlanMappingInventory> invs = new ArrayList<VxlanMappingInventory>(vos.size());
        for (VxlanMappingVO vo : vos) {
            invs.add(VxlanMappingInventory.valueOf(vo));
        }
        return invs;
    }

    public Integer getVni() {
        return vni;
    }

    public void setVni(Integer vni) {
        this.vni = vni;
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

