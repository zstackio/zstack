package org.zstack.sdnController.header;

import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Created by shixin.ruan on 07/09/2027.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@EO(EOClazz = L2NetworkEO.class, needView = false)
@AutoDeleteTag
public class HardwareL2VxlanNetworkVO extends VxlanNetworkVO {
    @Column
    private int vlan;

    public HardwareL2VxlanNetworkVO() {
    }

    public HardwareL2VxlanNetworkVO(L2NetworkVO vo) {
        super(vo);
    }

    public int getVlan() {
        return vlan;
    }

    public void setVlan(int vlan) {
        this.vlan = vlan;
    }
}
