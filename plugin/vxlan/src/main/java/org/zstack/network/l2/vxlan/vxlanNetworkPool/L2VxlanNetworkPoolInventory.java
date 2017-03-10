package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import com.mchange.v2.collection.MapEntry;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.network.l2.vxlan.vtep.VtepInventory;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetwork;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

import javax.persistence.JoinColumn;
import java.util.*;

@PythonClassInventory
@Inventory(mappingVOClass = VxlanNetworkPoolVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = L2NetworkInventory.class, type = VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vniRange", inventoryClass = VniRangeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "l2NetworkUuid"),
        @ExpandedQuery(expandedField = "l2VxlanNetwork", inventoryClass = L2VxlanNetworkInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "poolUuid"),
        @ExpandedQuery(expandedField = "vtep", inventoryClass = VtepInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "poolUuid")
})
public class L2VxlanNetworkPoolInventory extends L2NetworkInventory {
    @Queryable(mappingClass = VtepVO.class,
            joinColumn = @JoinColumn(name = "poolUuid", referencedColumnName = "uuid"))
    private Set<VtepVO> attachedVtepRefs;

    @Queryable(mappingClass = VxlanNetworkVO.class,
            joinColumn = @JoinColumn(name = "poolUuid", referencedColumnName = "uuid"))
    private Set<VxlanNetworkVO> attachedVxlanNetworkRefs;

    @Queryable(mappingClass = VniRangeVO.class,
            joinColumn = @JoinColumn(name = "l2NetworkUuid", referencedColumnName = "uuid"))
    private Set<VniRangeVO> attachedVniRanges;

    private Map<String, String> attachedCidrs;

    public L2VxlanNetworkPoolInventory() {
    }

    protected L2VxlanNetworkPoolInventory(VxlanNetworkPoolVO vo) {
        super(vo);
        this.attachedCidrs = VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTokensByResourceUuid(vo.getUuid());
        this.attachedVniRanges = vo.getAttachedVniRanges();
        this.attachedVtepRefs = vo.getAttachedVtepRefs();
        this.attachedVxlanNetworkRefs = vo.getAttachedVxlanNetworkRefs();
    }

    public static L2VxlanNetworkPoolInventory valueOf(VxlanNetworkPoolVO vo) {
        return new L2VxlanNetworkPoolInventory();
    }

    public static List<L2VxlanNetworkPoolInventory> valueOf1(Collection<VxlanNetworkPoolVO> vos) {
        List<L2VxlanNetworkPoolInventory> invs = new ArrayList<L2VxlanNetworkPoolInventory>(vos.size());
        for (VxlanNetworkPoolVO vo : vos) {
            invs.add(new L2VxlanNetworkPoolInventory(vo));
        }
        return invs;
    }

    public Set<VtepVO> getAttachedVtepRefs() {
        return attachedVtepRefs;
    }

    public void setAttachedVtepRefs(Set<VtepVO> attachedVtepRefs) {
        this.attachedVtepRefs = attachedVtepRefs;
    }

    public Set<VxlanNetworkVO> getAttachedVxlanNetworkRefs() {
        return attachedVxlanNetworkRefs;
    }

    public void setAttachedVxlanNetworkRefs(Set<VxlanNetworkVO> attachedVxlanNetworkRefs) {
        this.attachedVxlanNetworkRefs = attachedVxlanNetworkRefs;
    }

    public Set<VniRangeVO> getAttachedVniRanges() {
        return attachedVniRanges;
    }

    public void setAttachedVniRanges(Set<VniRangeVO> attachedVniRanges) {
        this.attachedVniRanges = attachedVniRanges;
    }

    public Map<String, String> getAttachedCidrs() {
        return attachedCidrs;
    }

    public void setAttachedCidrs(Map<String, String> attachedCidrs) {
        this.attachedCidrs = attachedCidrs;
    }
}
