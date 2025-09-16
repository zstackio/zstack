package org.zstack.sdnController.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.sdncontroller.SdnControllerInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = HardwareL2VxlanNetworkVO.class, collectionValueOfMethod = "valueOf2",
        parent = {@Parent(inventoryClass = L2NetworkInventory.class, type = SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(expandedField = "sdnController", inventoryClass = SdnControllerInventory.class,
                foreignKey = "sdnControllerUuid", expandedInventoryKey = "uuid"),
})
public class HardwareL2VxlanNetworkInventory extends L2VxlanNetworkInventory {
    private Integer vlan;

    public HardwareL2VxlanNetworkInventory() {
    }

    protected HardwareL2VxlanNetworkInventory(HardwareL2VxlanNetworkVO vo) {
        super(vo);
        this.setVlan(vo.getVlan());
    }

    public static HardwareL2VxlanNetworkInventory valueOf(HardwareL2VxlanNetworkVO vo) {
        return new HardwareL2VxlanNetworkInventory(vo);
    }

    public static List<HardwareL2VxlanNetworkInventory> valueOf2(Collection<HardwareL2VxlanNetworkVO> vos) {
        List<HardwareL2VxlanNetworkInventory> invs = new ArrayList<HardwareL2VxlanNetworkInventory>(vos.size());
        for (HardwareL2VxlanNetworkVO vo : vos) {
            invs.add(new HardwareL2VxlanNetworkInventory(vo));
        }
        return invs;
    }

    public Integer getVlan() {
        return vlan;
    }

    public void setVlan(Integer vlan) {
        this.vlan = vlan;
    }
}
