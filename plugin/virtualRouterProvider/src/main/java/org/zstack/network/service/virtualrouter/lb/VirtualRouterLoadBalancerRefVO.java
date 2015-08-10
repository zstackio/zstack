package org.zstack.network.service.virtualrouter.lb;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.network.service.lb.LoadBalancerVO;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Created by frank on 8/9/2015.
 */
@Entity
@Table
public class VirtualRouterLoadBalancerRefVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = VipVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.RESTRICT)
    private String vipUuid;

    @Column
    @ForeignKey(parentEntityClass = VirtualRouterVmVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String virtualRouterVmUuid;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String loadBalancerUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
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
