package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;

/**
 * Created by weiwang on 06/03/2017.
 */
@Inventory(mappingVOClass = VtepL2NetworkRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vtep", inventoryClass = VtepInventory.class,
                foreignKey = "vtepUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "l2Network", inventoryClass = L2VxlanNetworkInventory.class,
                foreignKey = "l2NetworkUuid", expandedInventoryKey = "uuid"),
})
public class VtepL2NetworkRefInventory {

    private String vtepUuid;

    private String l2NetworkUuid;

    public static VtepL2NetworkRefInventory valueOf(VtepL2NetworkRefVO vo) {
        VtepL2NetworkRefInventory inv = new VtepL2NetworkRefInventory();
        inv.setL2NetworkUuid(vo.getL2NetworkUuid());
        inv.setVtepUuid(vo.getVtepUuid());
        return inv;
    }

    public String getVtepUuid() {
        return vtepUuid;
    }

    public void setVtepUuid(String vtepUuid) {
        this.vtepUuid = vtepUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }
}
