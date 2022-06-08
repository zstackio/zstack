package org.zstack.header.vm.devices;

import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table
public class VmInstanceDeviceAddressGroupVO implements ToInventory {
    @Id
    @Column
    private String uuid;

    @Column
    private String resourceUuid;

    @Column
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "addressGroupUuid", insertable = false, updatable = false)
    private List<VmInstanceDeviceAddressArchiveVO> addressList;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public List<VmInstanceDeviceAddressArchiveVO> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<VmInstanceDeviceAddressArchiveVO> addressList) {
        this.addressList = addressList;
    }
}
