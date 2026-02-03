package org.zstack.header.tpm.entity;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;
import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@BaseResource
@AutoDeleteTag
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = VmInstanceEO.class, joinColumn = "vmInstanceUuid")
})
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid"),
        }
)
public class TpmVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "tpmUuid", insertable = false, updatable = false)
    @NoView
    private Set<TpmHostRefVO> hostRefs = new HashSet<>();

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
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

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Set<TpmHostRefVO> getHostRefs() {
        return hostRefs;
    }

    public void setHostRefs(Set<TpmHostRefVO> hostRefs) {
        this.hostRefs = hostRefs;
    }

    public TpmVO() {
    }

    @Override
    public String toString() {
        return "TpmVO{" +
        "vmInstanceUuid='" + vmInstanceUuid + '\'' +
        ", uuid='" + uuid + '\'' +
        '}';
    }
}
