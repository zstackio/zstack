package org.zstack.network.l2.vxlan.vxlanNetworkPool;


import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.network.l2.vxlan.vtep.VtepL2NetworkRefVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by weiwang on 02/03/2017.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@EO(EOClazz = L2NetworkEO.class, needView = false)
@AutoDeleteTag
public class VxlanNetworkPoolVO extends L2NetworkVO {
    @Column
    private int startVni;

    @Column
    private int endVni;

    @Column
    private String vtepCidr;

    public Set<VtepL2NetworkRefVO> getAttachedVtepRefs() {
        return attachedVtepRefs;
    }

    public void setAttachedVtepRefs(Set<VtepL2NetworkRefVO> attachedVtepRefs) {
        this.attachedVtepRefs = attachedVtepRefs;
    }

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vtepUuid", insertable = false, updatable = false)
    @NoView
    private Set<VtepL2NetworkRefVO> attachedVtepRefs = new HashSet<>();


    public VxlanNetworkPoolVO() {
    }

    public VxlanNetworkPoolVO(L2NetworkVO vo) {
        super(vo);
    }

    public int getStartVni() {
        return startVni;
    }

    public int getEndVni() {
        return endVni;
    }

    public String getVtepCidr() {
        return vtepCidr;
    }

    public void setStartVni(int startVni) {
        this.startVni = startVni;
    }

    public void setEndVni(int endVni) {
        this.endVni = endVni;
    }

    public void setVtepCidr(String vtepCidr) {
        this.vtepCidr = vtepCidr;
    }
}
