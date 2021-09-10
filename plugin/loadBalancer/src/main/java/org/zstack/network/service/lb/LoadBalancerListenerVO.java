package org.zstack.network.service.lb;

import org.zstack.core.db.Q;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by frank on 8/8/2015.
 */
@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = LoadBalancerVO.class, myField = "loadBalancerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = LoadBalancerListenerVmNicRefVO.class, myField = "uuid", targetField = "listenerUuid"),
                @EntityGraph.Neighbour(type = LoadBalancerListenerCertificateRefVO.class, myField = "uuid", targetField = "listenerUuid"),
                @EntityGraph.Neighbour(type = LoadBalancerListenerACLRefVO.class, myField = "uuid", targetField = "listenerUuid")
        }
)
public class LoadBalancerListenerVO extends ResourceVO implements OwnedByAccount {
    @Column
    @ForeignKey(parentEntityClass = LoadBalancerVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.RESTRICT)
    private String loadBalancerUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private int instancePort;

    @Column
    private int loadBalancerPort;

    @Column
    private String protocol;

    @Column
    private String securityPolicyType;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="listenerUuid", insertable=false, updatable=false)
    @NoView
    private Set<LoadBalancerListenerVmNicRefVO> vmNicRefs = new HashSet<LoadBalancerListenerVmNicRefVO>();

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="listenerUuid", insertable=false, updatable=false)
    @NoView
    private Set<LoadBalancerListenerACLRefVO> aclRefs = new HashSet<LoadBalancerListenerACLRefVO>();

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="listenerUuid", insertable=false, updatable=false)
    @NoView
    private Set<LoadBalancerListenerCertificateRefVO> certificateRefs;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }


    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Set<LoadBalancerListenerVmNicRefVO> getVmNicRefs() {
        return vmNicRefs;
    }

    public void setVmNicRefs(Set<LoadBalancerListenerVmNicRefVO> vmNicRefs) {
        this.vmNicRefs = vmNicRefs;
    }

    public Set<LoadBalancerListenerACLRefVO> getAclRefs() {
        return aclRefs;
    }

    public void setAclRefs(Set<LoadBalancerListenerACLRefVO> aclRefs) {
        this.aclRefs = aclRefs;
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

    public int getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(int instancePort) {
        this.instancePort = instancePort;
    }

    public int getLoadBalancerPort() {
        return loadBalancerPort;
    }

    public void setLoadBalancerPort(int loadBalancerPort) {
        this.loadBalancerPort = loadBalancerPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSecurityPolicyType() {
        return securityPolicyType;
    }

    public void setSecurityPolicyType(String securityPolicyType) {
        this.securityPolicyType = securityPolicyType;
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

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public Set<LoadBalancerListenerCertificateRefVO> getCertificateRefs() {
        return certificateRefs;
    }

    public void setCertificateRefs(Set<LoadBalancerListenerCertificateRefVO> certificateRefs) {
        this.certificateRefs = certificateRefs;
    }
}
