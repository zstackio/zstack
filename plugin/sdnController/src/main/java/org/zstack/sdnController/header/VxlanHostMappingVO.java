package org.zstack.sdnController.header;

import org.zstack.header.host.HostEO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = VxlanNetworkVO.class, myField = "vxlanUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = HostEO.class, myField = "hostUuid", targetField = "uuid"),
        }
)
public class VxlanHostMappingVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = VxlanNetworkVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vxlanUuid;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    private Integer vlanId;

    @Column
    private String physicalInterface;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVxlanUuid() {
        return vxlanUuid;
    }

    public void setVxlanUuid(String vxlanUuid) {
        this.vxlanUuid = vxlanUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
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

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;
}