package org.zstack.sdnController.header;

import org.zstack.header.network.l3.IpRangeEO;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by boce.wang on 07/02/2025.
 */

@Entity
@Table
@org.zstack.header.vo.EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = SdnControllerVO.class, myField = "sdnControllerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = IpRangeEO.class, myField = "ipRangeUuid", targetField = "uuid")
        }
)
public class H3cSdnSubnetIpRangeRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = SdnControllerVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String sdnControllerUuid;

    @Column
    @ForeignKey(parentEntityClass = IpRangeEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String ipRangeUuid;

    @Column
    private String subnetUuid;

    @Column
    private String l2NetworkUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    public String getIpRangeUuid() {
        return ipRangeUuid;
    }

    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }

    public String getSubnetUuid() {
        return subnetUuid;
    }

    public void setSubnetUuid(String subnetUuid) {
        this.subnetUuid = subnetUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
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
}
