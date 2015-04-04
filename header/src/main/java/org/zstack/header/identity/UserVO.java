package org.zstack.header.identity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@Inheritance(strategy=InheritanceType.JOINED)
public class UserVO {
    @Id
    @Column
    private String uuid;
    
    @Column
    private String accountUuid;
    
    @Column
    private String name;
    
    @Column
    private String password;
    
    @Column
    private String securityKey;
    
    @Column
    private String token;
    
    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;
    
    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(
        name="UserPolicyRefVO",
        joinColumns={@JoinColumn(name="userUuid", referencedColumnName="uuid", insertable=false, updatable=false)},
        inverseJoinColumns={@JoinColumn(name="policyUuid", referencedColumnName="uuid", insertable=false, updatable=false)})
    private Set<PolicyVO> policies = new HashSet<PolicyVO>();
    
    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(
        name="UserGroupUserRefVO",
        joinColumns={@JoinColumn(name="userUuid", referencedColumnName="uuid", insertable=false, updatable=false)},
        inverseJoinColumns={@JoinColumn(name="groupUuid", referencedColumnName="uuid", insertable=false, updatable=false)})
    private Set<UserGroupVO> groups = new HashSet<UserGroupVO>();

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getSecurityKey() {
        return securityKey;
    }

    public void setSecurityKey(String securityKey) {
        this.securityKey = securityKey;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Set<PolicyVO> getPolicies() {
        return policies;
    }

    public void setPolicies(Set<PolicyVO> policies) {
        this.policies = policies;
    }

    public Set<UserGroupVO> getGroups() {
        return groups;
    }

    public void setGroups(Set<UserGroupVO> groups) {
        this.groups = groups;
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
