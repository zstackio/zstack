package org.zstack.network.service.lb;

import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;


@Entity
@Table
@EntityGraph(

        parents = {
                @EntityGraph.Neighbour(type = LoadBalancerServerGroupVO.class, myField = "listenerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VmNicVO.class, myField = "vmNicUuid", targetField = "uuid"),
        }
)
public class LoadBalancerServerGroupVmNicRefVO {
    @Id
    @Column
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = LoadBalancerServerGroupVO.class, parentKey = "uuid", onDeleteAction = org.zstack.header.vo.ForeignKey.ReferenceOption.CASCADE)
    private String loadBalancerServerGroupUuid;

    @Column
    @ForeignKey(parentEntityClass = VmNicVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmNicUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private LoadBalancerVmNicStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public LoadBalancerVmNicStatus getStatus() {
        return status;
    }

    public void setStatus(LoadBalancerVmNicStatus status) {
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
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

    public String getLoadBalancerServerGroupUuid() {
        return loadBalancerServerGroupUuid;
    }

    public void setLoadBalancerServerGroupUuid(String loadBalancerServerGroupUuid) {
        this.loadBalancerServerGroupUuid = loadBalancerServerGroupUuid;
    }
}
