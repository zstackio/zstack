package org.zstack.header.host;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "uuid", targetField = "uuid")
        }
)
public class HostNUMATopologyVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String uuid;

    @Column
    private Integer nodeID;

    @Column
    private String nodeCPUs;

    @Column
    private Long nodeMemSize;

    @Column
    private String nodeDistance;

    public HostNUMATopologyVO() {};

    public void setId(Long id) {
        this.id = id;
    }

    public void setNodeCPUs(String nodeCPUs) {
        this.nodeCPUs = nodeCPUs;
    }

    public void setNodeID(String nodeID) {
        this.nodeID = Integer.parseInt(nodeID);
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;;
    }

    public void setNodeDistance(String nodeDistance) {
        this.nodeDistance = nodeDistance;
    }

    public void setNodeMemSize(Long nodeMemSize) {
        this.nodeMemSize = nodeMemSize;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public Long getId() {
        return id;
    }

    public Long getNodeMemSize() {
        return nodeMemSize;
    }

    public String getNodeCPUs() {
        return nodeCPUs;
    }

    public String getNodeDistance() {
        return nodeDistance;
    }

    public Integer getNodeID() {
        return nodeID;
    }
}