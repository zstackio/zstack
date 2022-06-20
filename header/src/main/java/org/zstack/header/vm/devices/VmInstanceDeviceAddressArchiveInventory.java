package org.zstack.header.vm.devices;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by LiangHanYu on 2022/6/17 17:24
 */
@Inventory(mappingVOClass = VmInstanceDeviceAddressArchiveVO.class)
public class VmInstanceDeviceAddressArchiveInventory {
    private long id;
    private String resourceUuid;
    private String vmInstanceUuid;
    private String pciAddress;
    private String addressGroupUuid;
    private String metadata;
    private String metadataClass;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VmInstanceDeviceAddressArchiveInventory valueOf(VmInstanceDeviceAddressArchiveVO vo) {
        VmInstanceDeviceAddressArchiveInventory inv = new VmInstanceDeviceAddressArchiveInventory();
        inv.setId(1);
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setPciAddress(vo.getPciAddress());
        inv.setAddressGroupUuid(vo.getAddressGroupUuid());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setMetadata(vo.getMetadata());
        inv.setMetadataClass(vo.getMetadataClass());
        return inv;
    }

    public static List<VmInstanceDeviceAddressArchiveInventory> valueOf(Collection<VmInstanceDeviceAddressArchiveVO> vos) {
        return vos.stream().map(VmInstanceDeviceAddressArchiveInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getPciAddress() {
        return pciAddress;
    }

    public void setPciAddress(String pciAddress) {
        this.pciAddress = pciAddress;
    }

    public String getAddressGroupUuid() {
        return addressGroupUuid;
    }

    public void setAddressGroupUuid(String addressGroupUuid) {
        this.addressGroupUuid = addressGroupUuid;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getMetadataClass() {
        return metadataClass;
    }

    public void setMetadataClass(String metadataClass) {
        this.metadataClass = metadataClass;
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
