package org.zstack.sdnController.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

@PythonClassInventory
@Inventory(mappingVOClass = VxlanClusterMappingVO.class, collectionValueOfMethod = "valueOf1")
public class VxlanClusterMappingInventory implements Serializable {
    private String vxlanUuid;
    private String clusterUuid;
    private Integer vlanId;
    private String physicalInterface;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VxlanClusterMappingInventory() {}

    protected VxlanClusterMappingInventory(VxlanClusterMappingVO vo) {
        this.vxlanUuid = vo.getVxlanUuid();
        this.clusterUuid = vo.getClusterUuid();
        this.vlanId = vo.getVlanId();
        this.physicalInterface = vo.getPhysicalInterface();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static VxlanClusterMappingInventory valueOf(VxlanClusterMappingVO vo) {
        return new VxlanClusterMappingInventory(vo);
    }

    public static VxlanClusterMappingInventory valueOf2(Map<String,String> keyMap, Map<Integer,String> valueMap) {
        VxlanClusterMappingInventory inv = new VxlanClusterMappingInventory();
        Optional<Map.Entry<String, String>> key = keyMap.entrySet().stream().findFirst();
        if (key.isPresent()) {
            inv.setClusterUuid(key.get().getKey());
            inv.setPhysicalInterface(key.get().getValue());
        }
        Optional<Map.Entry<Integer, String>> value = valueMap.entrySet().stream().findFirst();
        if (value.isPresent()) {
            inv.setVlanId(value.get().getKey());
            inv.setPhysicalInterface(value.get().getValue());
        }
        return inv;
    }

    public static List<VxlanClusterMappingInventory> valueOf1(Collection<VxlanClusterMappingVO> vos) {
        List<VxlanClusterMappingInventory> invs = new ArrayList<VxlanClusterMappingInventory>(vos.size());
        for (VxlanClusterMappingVO vo : vos) {
            invs.add(VxlanClusterMappingInventory.valueOf(vo));
        }
        return invs;
    }
    public String getVxlanUuid() {
        return vxlanUuid;
    }

    public void setVxlanUuid(String vxlanUuid) {
        this.vxlanUuid = vxlanUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
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

