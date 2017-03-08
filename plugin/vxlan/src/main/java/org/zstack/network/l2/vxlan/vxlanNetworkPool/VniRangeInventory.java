package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

import javax.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by weiwang on 08/03/2017.
 */
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vxlanPool", inventoryClass = L2VxlanNetworkPoolInventory.class,
                foreignKey = "poolUuid", expandedInventoryKey = "uuid")
})
public class VniRangeInventory {
    private String uuid;

    private String name;

    private String description;

    private Integer startVni;

    private Integer endVni;

    @Queryable(mappingClass = VxlanNetworkVO.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "poolUuid"))
    private String poolUuid;

    public VniRangeInventory() {
    }

    protected VniRangeInventory(VniRangeVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.startVni = vo.getStartVni();
        this.endVni = vo.getEndVni();
        this.poolUuid = vo.getPoolUuid();
    }

    public static VniRangeInventory valueOf(VniRangeVO vo) {
        return new VniRangeInventory();
    }

    public static List<VniRangeInventory> valueOf(Collection<VniRangeVO> vos) {
        List<VniRangeInventory> invs = new ArrayList<>();
        for (VniRangeVO vo : vos) {
            invs.add(new VniRangeInventory(vo));
        }
        return invs;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
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

    public Integer getStartVni() {
        return startVni;
    }

    public void setStartVni(Integer startVni) {
        this.startVni = startVni;
    }

    public Integer getEndVni() {
        return endVni;
    }

    public void setEndVni(Integer endVni) {
        this.endVni = endVni;
    }
}
