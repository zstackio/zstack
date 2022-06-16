package org.zstack.header.vm.devices;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by LiangHanYu on 2022/6/20 18:03
 */
@Inventory(mappingVOClass = VmInstanceDeviceAddressGroupVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volumeSnapshotRef", inventoryClass = VmInstanceDeviceAddressArchiveInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "addressGroupUuid", hidden = true),
})
public class VmInstanceDeviceAddressGroupInventory {
    private String uuid;
    private String resourceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @Queryable(mappingClass = VmInstanceDeviceAddressArchiveInventory.class,
            joinColumn = @JoinColumn(name = "addressGroupUuid"))
    private List<VmInstanceDeviceAddressArchiveInventory> addressList;

    public static VmInstanceDeviceAddressGroupInventory valueOf(VmInstanceDeviceAddressGroupVO vo) {
        VmInstanceDeviceAddressGroupInventory inv = new VmInstanceDeviceAddressGroupInventory();
        inv.setUuid(vo.getUuid());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setAddressList(VmInstanceDeviceAddressArchiveInventory.valueOf(vo.getAddressList()));
        return inv;
    }

    public static List<VmInstanceDeviceAddressGroupInventory> valueOf(Collection<VmInstanceDeviceAddressGroupVO> vos) {
        return vos.stream().map(VmInstanceDeviceAddressGroupInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public List<VmInstanceDeviceAddressArchiveInventory> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<VmInstanceDeviceAddressArchiveInventory> addressList) {
        this.addressList = addressList;
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
