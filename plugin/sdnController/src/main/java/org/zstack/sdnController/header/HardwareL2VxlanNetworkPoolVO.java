package org.zstack.sdnController.header;

import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;

import javax.persistence.*;

/**
 * Created by shixin.ruan on 09/30/2019.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@EO(EOClazz = L2NetworkEO.class, needView = false)
@AutoDeleteTag
public class HardwareL2VxlanNetworkPoolVO extends VxlanNetworkPoolVO {
    @Column
    @ForeignKey(parentEntityClass = SdnControllerVO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String sdnControllerUuid;

    public HardwareL2VxlanNetworkPoolVO() {
    }

    public HardwareL2VxlanNetworkPoolVO(L2NetworkVO vo) {
        super(vo);
    }

    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }
}
