package org.zstack.header.tpm.entity;

import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transform;

@Inventory(mappingVOClass = TpmHostRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "tpm", inventoryClass = TpmInventory.class,
                foreignKey = "tpmUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
})
public class TpmHostRefInventory {
    private long id;
    private String tpmUuid;
    private String hostUuid;
    private String path;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public TpmHostRefInventory() {
    }

    public static TpmHostRefInventory valueOf(TpmHostRefVO vo) {
        TpmHostRefInventory inv = new TpmHostRefInventory();
        inv.setId(vo.getId());
        inv.setTpmUuid(vo.getTpmUuid());
        inv.setHostUuid(vo.getHostUuid());
        inv.setPath(vo.getPath());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<TpmHostRefInventory> valueOf(Collection<TpmHostRefVO> vos) {
        return transform(vos, TpmHostRefInventory::valueOf);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTpmUuid() {
        return tpmUuid;
    }

    public void setTpmUuid(String tpmUuid) {
        this.tpmUuid = tpmUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public static TpmHostRefInventory __example__() {
        TpmHostRefInventory ref = new TpmHostRefInventory();
        ref.setId(1L);
        ref.setTpmUuid(DocUtils.createFixedUuid(TpmVO.class));
        ref.setHostUuid(DocUtils.createFixedUuid(HostVO.class));
        ref.setCreateDate(DocUtils.timestamp());
        ref.setLastOpDate(DocUtils.timestamp());
        return ref;
    }
}
