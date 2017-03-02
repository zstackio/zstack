package org.zstack.network.l2.vxlan.vxlanNetworkPool;


import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

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

    public VxlanNetworkPoolVO() {
    }

    public VxlanNetworkPoolVO(L2NetworkVO vo) {
        super(vo);
    }

    public int getStartVni() {
        return startVni;
    }

    public int getendVni() {
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
