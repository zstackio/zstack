package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.vo.*;

import javax.persistence.*;
import javax.persistence.ForeignKey;

/**
 * Created by weiwang on 02/03/2017.
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = L2NetworkVO.class, joinColumn = "l2NetworkUuid"),
        @SoftDeletionCascade(parent = VtepVO.class, joinColumn = "vtepUuid")
})
public class VtepL2NetworkRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = VtepVO.class, onDeleteAction = org.zstack.header.vo.ForeignKey.ReferenceOption.CASCADE)
    private String vtepUuid;

    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = L2NetworkEO.class, onDeleteAction = org.zstack.header.vo.ForeignKey.ReferenceOption.CASCADE)
    private String l2NetworkUuid;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getVtepUuid() {
        return vtepUuid;
    }

    public void setVtepUuid(String vtepUuid) {
        this.vtepUuid = vtepUuid;
    }
}
