package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.header.network.l2.L2Network;
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
public class VxlanNetworkVO extends L2NetworkVO {
    @Column
    private int vni;

    @Column
    private String poolUuid;

    @Column
    private String vtepCidr;

    public VxlanNetworkVO() {
    }

    public VxlanNetworkVO(L2NetworkVO vo) {
        super(vo);
    }

    public int getVni() {
        return vni;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public String getVtepCidr() {
        return vtepCidr;
    }

    public void setVni(int vni) {
        this.vni = vni;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public void setVtepCidr(String vtepCidr) {
        this.vtepCidr = vtepCidr;
    }

}
