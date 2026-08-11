package org.zstack.network.service.lb;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {
        "listenerUuid", "serverGroupUuid", "serverIpId"
}))
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = LoadBalancerListenerVO.class, myField = "listenerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = LoadBalancerServerGroupVO.class, myField = "serverGroupUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = LoadBalancerServerGroupServerIpVO.class, myField = "serverIpId", targetField = "id"),
        }
)
public class LoadBalancerListenerServerGroupServerIpRefVO {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerListenerVO.class, parentKey = "uuid",
            onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String listenerUuid;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerServerGroupVO.class, parentKey = "uuid",
            onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String serverGroupUuid;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerServerGroupServerIpVO.class, parentKey = "id",
            onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private long serverIpId;

    @Column
    @Enumerated(EnumType.STRING)
    private LoadBalancerBackendServerState state;

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

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

    public long getServerIpId() {
        return serverIpId;
    }

    public void setServerIpId(long serverIpId) {
        this.serverIpId = serverIpId;
    }

    public LoadBalancerBackendServerState getState() {
        return state;
    }

    public void setState(LoadBalancerBackendServerState state) {
        this.state = state;
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
