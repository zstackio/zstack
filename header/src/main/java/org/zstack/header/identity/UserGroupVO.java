package org.zstack.header.identity;

import org.zstack.header.search.SqlTrigger;
import org.zstack.header.search.TriggerIndex;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@Inheritance(strategy=InheritanceType.JOINED)
@TriggerIndex
@SqlTrigger
public class UserGroupVO {
    @Id
    @Column
    private String uuid;
    
    @Column
    private String accountUuid;
    
    @Column
    private String name;
    
    @Column
    private String description;
    
    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;

    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(
        name="UserGroupPolicyRefVO",
        joinColumns={@JoinColumn(name="groupUuid", referencedColumnName="uuid", insertable=false, updatable=false)},
        inverseJoinColumns={@JoinColumn(name="policyUuid", referencedColumnName="uuid", insertable=false, updatable=false)})
    private Set<PolicyVO> policies = new HashSet<PolicyVO>();
    
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
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

    public Set<PolicyVO> getPolicies() {
        return policies;
    }

    public void setPolicies(Set<PolicyVO> policies) {
        this.policies = policies;
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
