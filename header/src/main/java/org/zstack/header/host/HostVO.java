package org.zstack.header.host;

import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;

@Entity
@Table
@EO(EOClazz = HostEO.class)
@AutoDeleteTag
public class HostVO extends HostAO {
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uuid")
    @NoView
    private HostCapacityVO capacity;

    public HostCapacityVO getCapacity() {
        return capacity;
    }

    public void setCapacity(HostCapacityVO capacity) {
        this.capacity = capacity;
    }

    public HostVO() {
    }

    protected HostVO(HostVO vo) {
        this.setClusterUuid(vo.getClusterUuid());
        this.setStatus(vo.getStatus());
        this.setCreateDate(vo.getCreateDate());
        this.setDescription(vo.getDescription());
        this.setHypervisorType(vo.getHypervisorType());
        this.setLastOpDate(vo.getLastOpDate());
        this.setManagementIp(vo.getManagementIp());
        this.setName(vo.getName());
        this.setState(vo.getState());
        this.setUuid(vo.getUuid());
        this.setZoneUuid(vo.getZoneUuid());
        this.setCapacity(vo.getCapacity());
    }
}

