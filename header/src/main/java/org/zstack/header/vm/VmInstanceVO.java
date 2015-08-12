package org.zstack.header.vm;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@Inheritance(strategy=InheritanceType.JOINED)
@EO(EOClazz = VmInstanceEO.class)
@AutoDeleteTag
public class VmInstanceVO extends VmInstanceAO {
    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="vmInstanceUuid", insertable=false, updatable=false)
    @NoView
    private Set<VmNicVO> vmNics = new HashSet<VmNicVO>();
    
    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="vmInstanceUuid", insertable=false, updatable=false)
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
}
