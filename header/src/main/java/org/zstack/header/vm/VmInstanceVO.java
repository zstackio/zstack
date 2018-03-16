package org.zstack.header.vm;

import org.zstack.header.hierarchy.EntityHierarchy;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = VmInstanceEO.class)
@BaseResource
@EntityHierarchy(
        parent = ZoneVO.class,
        myField = "zoneUuid",
        targetField = "uuid"
)
public class VmInstanceVO extends VmInstanceAO {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vmInstanceUuid", insertable = false, updatable = false)
    @NoView
    private Set<VmNicVO> vmNics = new HashSet<VmNicVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vmInstanceUuid", insertable = false, updatable = false)
    @NoView
    private Set<VolumeVO> allVolumes = new HashSet<VolumeVO>();

    public VmInstanceVO() {
    }

    public VmInstanceVO(VmInstanceVO other) {
        super(other);
        this.vmNics = other.vmNics;
        this.allVolumes = other.allVolumes;
    }

    public Set<VmNicVO> getVmNics() {
        return vmNics;
    }

    public void setVmNics(Set<VmNicVO> vmNics) {
        this.vmNics = vmNics;
    }

    public Set<VolumeVO> getAllVolumes() {
        return allVolumes;
    }

    public void setAllVolumes(Set<VolumeVO> allVolumes) {
        this.allVolumes = allVolumes;
    }

    public VolumeVO getRootVolume() {
        if (allVolumes == null) {
            return null;
        }

        Optional<VolumeVO> opt = allVolumes.stream().filter(v -> v.getUuid().equals(getRootVolumeUuid())).findAny();
        return opt.isPresent() ? opt.get() : null;
    }
}
