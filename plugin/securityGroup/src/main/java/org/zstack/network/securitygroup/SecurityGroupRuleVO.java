package org.zstack.network.securitygroup;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
public class SecurityGroupRuleVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = SecurityGroupVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String securityGroupUuid;
    
    @Column
    @Enumerated(EnumType.STRING)
    private SecurityGroupRuleType type;
    
    @Column
    private int startPort;
    
    @Column
    private int endPort;
    
    @Column
    @Enumerated(EnumType.STRING)
    private SecurityGroupRuleProtocolType protocol;

    @Column
    @Enumerated(EnumType.STRING)
    private SecurityGroupRuleState state = SecurityGroupRuleState.Enabled;

    @Column
    private String allowedCidr;

    @Column
    @ForeignKey(parentEntityClass = SecurityGroupVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String remoteSecurityGroupUuid;
    
    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public SecurityGroupRuleState getState() {
        return state;
    }

    public void setState(SecurityGroupRuleState state) {
        this.state = state;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public SecurityGroupRuleType getType() {
        return type;
    }

    public void setType(SecurityGroupRuleType type) {
        this.type = type;
    }

    public int getStartPort() {
        return startPort;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }
    
    public SecurityGroupRuleProtocolType getProtocol() {
        return protocol;
    }

    public void setProtocol(SecurityGroupRuleProtocolType protocol) {
        this.protocol = protocol;
    }

    public String getAllowedCidr() {
        return allowedCidr;
    }

    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
    }

    public void setRemoteSecurityGroupUuid(String remoteSecurityGroupUuid) {
        this.remoteSecurityGroupUuid = remoteSecurityGroupUuid;
    }

    public String getRemoteSecurityGroupUuid() {
        return remoteSecurityGroupUuid;
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
