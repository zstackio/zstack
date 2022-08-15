package org.zstack.sdnController.header;

import org.zstack.core.db.Q;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.*;

import java.util.*;

@PythonClassInventory
@Inventory(mappingVOClass = HardwareL2VxlanNetworkPoolVO.class, collectionValueOfMethod = "valueOf2",
        parent = {@Parent(inventoryClass = L2NetworkInventory.class, type = SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vniRange", inventoryClass = VniRangeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "l2NetworkUuid"),
        @ExpandedQuery(expandedField = "l2VxlanNetwork", inventoryClass = L2VxlanNetworkInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "poolUuid"),
        @ExpandedQuery(expandedField = "sdnController", inventoryClass = SdnControllerInventory.class,
                foreignKey = "sdnControllerUuid", expandedInventoryKey = "uuid"),
})
public class HardwareL2VxlanNetworkPoolInventory extends L2VxlanNetworkPoolInventory {
    private String sdnControllerUuid;

    public HardwareL2VxlanNetworkPoolInventory() {
    }

    protected HardwareL2VxlanNetworkPoolInventory(HardwareL2VxlanNetworkPoolVO vo) {
        super(vo);
        setSdnControllerUuid(vo.getSdnControllerUuid());
        setAttachedVniRanges(VniRangeInventory.valueOf(vo.getAttachedVniRanges()));
        List<VxlanNetworkVO> networkVOS = Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.poolUuid, vo.getUuid()).list();
        setAttachedVxlanNetworkRefs(L2VxlanNetworkInventory.valueOf1(networkVOS));
    }

    public static HardwareL2VxlanNetworkPoolInventory valueOf(HardwareL2VxlanNetworkPoolVO vo) {
        return new HardwareL2VxlanNetworkPoolInventory(vo);
    }

    public static List<HardwareL2VxlanNetworkPoolInventory> valueOf2(Collection<HardwareL2VxlanNetworkPoolVO> vos) {
        List<HardwareL2VxlanNetworkPoolInventory> invs = new ArrayList<HardwareL2VxlanNetworkPoolInventory>(vos.size());
        for (HardwareL2VxlanNetworkPoolVO vo : vos) {
            invs.add(new HardwareL2VxlanNetworkPoolInventory(vo));
        }
        return invs;
    }

    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }
}
