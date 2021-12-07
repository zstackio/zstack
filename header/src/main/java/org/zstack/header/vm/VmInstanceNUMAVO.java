package org.zstack.header.vm;


import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "uuid", targetField = "uuid")
        }
)
public class VmInstanceNUMAVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String uuid;

    @Column
    private Integer vNodeID;

    @Column
    private String vNodeCPUs;

    @Column
    private Long vNodeMemSize;

    @Column
    private String vNodeDistance;

    @Column
    private Integer pNodeID;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setpNodeID(Integer pNodeID) {
        this.pNodeID = pNodeID;
    }

    public void setvNodeCPUs(String vNodeCPUs) {
        this.vNodeCPUs = vNodeCPUs;
    }

    public void setvNodeDistance(String vNodeDistance) {
        this.vNodeDistance = vNodeDistance;
    }

    public void setvNodeID(Integer vNodeID) {
        this.vNodeID = vNodeID;
    }

    public void setvNodeMemSize(Long vNodeMemSize) {
        this.vNodeMemSize = vNodeMemSize;
    }

    public String getUuid() {
        return uuid;
    }

    public Integer getpNodeID() {
        return pNodeID;
    }

    public Integer getvNodeID() {
        return vNodeID;
    }

    public Long getvNodeMemSize() {
        return vNodeMemSize;
    }

    public String getvNodeCPUs() {
        return vNodeCPUs;
    }

    public String getvNodeDistance() {
        return vNodeDistance;
    }
}
