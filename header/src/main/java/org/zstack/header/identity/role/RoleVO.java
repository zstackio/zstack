package org.zstack.header.identity.role;

import org.zstack.header.host.HostVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.identity.PolicyVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table
@BaseResource
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = RolePolicyRefVO.class, myField = "uuid", targetField = "roleUuid"),
        }
)
public class RoleVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String identity;
    @Column
    private String rootUuid;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;
    @Column
    @Enumerated(EnumType.STRING)
    private RoleState state = RoleState.Enabled;
    @Column
    @Enumerated(EnumType.STRING)
    private RoleType type;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "roleUuid", insertable = false, updatable = false)
    private Set<RolePolicyStatementVO> statements = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "roleUuid", insertable = false, updatable = false)
    private Set<RolePolicyRefVO> policies = new HashSet<>();

    @Transient
    private String accountUuid;

    public RoleVO copy() {
        RoleVO vo = new RoleVO();
        vo.name = name;
        vo.uuid = UUID.randomUUID().toString().replace("-", "");
        vo.state = RoleState.Enabled;
        vo.description = description;
        vo.type = type;
        vo.identity = identity;
        vo.rootUuid = rootUuid;
        return vo;
    }

    public RoleVO() {
    }

    protected RoleVO(RoleVO vo) {
        this.name = vo.getName();
        this.uuid = vo.getUuid();
        this.state = vo.getState();
        this.description = vo.getDescription();
        this.type = vo.getType();
        this.identity = vo.getIdentity();
        this.rootUuid = vo.getRootUuid();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public RoleState getState() {
        return state;
    }

    public void setState(RoleState state) {
        this.state = state;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Set<RolePolicyStatementVO> getStatements() {
        return statements;
    }

    public void setStatements(Set<RolePolicyStatementVO> statements) {
        this.statements = statements;
    }

    public Set<RolePolicyRefVO> getPolicies() {
        return policies;
    }

    public void setPolicies(Set<RolePolicyRefVO> policies) {
        this.policies = policies;
    }

    public RoleType getType() {
        return type;
    }

    public void setType(RoleType type) {
        this.type = type;
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

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getRootUuid() {
        return rootUuid;
    }

    public void setRootUuid(String rootUuid) {
        this.rootUuid = rootUuid;
    }
}
