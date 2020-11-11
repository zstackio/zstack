package org.zstack.network.service.lb;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
@EntityGraph(

        parents = {
                @EntityGraph.Neighbour(type = LoadBalancerServerGroupVO.class, myField = "loadBalancerServerGroupUuid", targetField = "uuid"),
        }
)
public class LoadBalancerServerGroupServerIpVO  {
    @Id
    @Column
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    @Column
    private String ipAddress;

    @Column
    private Long weight;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerServerGroupVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String loadBalancerServerGroupUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private LoadBalancerBackendServerStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLoadBalancerServerGroupUuid() {
        return loadBalancerServerGroupUuid;
    }

    public void setLoadBalancerServerGroupUuid(String loadBalancerServerGroupUuid) {
        this.loadBalancerServerGroupUuid = loadBalancerServerGroupUuid;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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

    public LoadBalancerBackendServerStatus getStatus() {
        return status;
    }

    public void setStatus(LoadBalancerBackendServerStatus status) {
        this.status = status;
    }

    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }
}
