package org.zstack.network.service.lb;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.network.service.vip.VipVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by frank on 8/7/2015.
 */
@Entity
@Table
@BaseResource
public class LoadBalancerVO extends ResourceVO {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String providerType;

    @Column
    @Enumerated(EnumType.STRING)
    private LoadBalancerState state;

    @Column
    @ForeignKey(parentEntityClass = VipVO.class, parentKey = "uuid")
    private String vipUuid;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="loadBalancerUuid", insertable=false, updatable=false)
    @NoView
    private Set<LoadBalancerListenerVO> listeners = new HashSet<LoadBalancerListenerVO>();

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Set<LoadBalancerListenerVO> getListeners() {
        return listeners;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public void setListeners(Set<LoadBalancerListenerVO> listeners) {
        this.listeners = listeners;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LoadBalancerState getState() {
        return state;
    }

    public void setState(LoadBalancerState state) {
        this.state = state;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
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
