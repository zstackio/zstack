package org.zstack.network.service.lb;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.network.service.vip.VipVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by frank on 8/8/2015.
 */
@Entity
@Table
public class LoadBalancerVipRefVO {
    @Id
    @Column
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    @Column
    @ForeignKey(parentEntityClass = VipVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String vipUuid;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String loadBalancerUuid;

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

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
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
